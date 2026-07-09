package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.CancelRideRequest;
import com.akash.pooler_backend.dto.request.UpdateRideStatusRequest;
import com.akash.pooler_backend.dto.response.RideResponse;
import com.akash.pooler_backend.dto.response.ArrivalConfirmationResponse;
import com.akash.pooler_backend.dto.response.RouteCompatibilityResponse;
import com.akash.pooler_backend.entity.PbRideEntity;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.RideStatus;
import com.akash.pooler_backend.exception.IncompatibleRouteException;
import com.akash.pooler_backend.exception.RideForbiddenException;
import com.akash.pooler_backend.exception.RideInvalidStateException;
import com.akash.pooler_backend.exception.RideNotFoundException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbRideRepository;
import com.akash.pooler_backend.service.GeoService;
import com.akash.pooler_backend.service.RideService;
import com.akash.pooler_backend.utils.GeoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * @author Akash Kumar
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RideServiceImpl implements RideService {

    /** Naive fare model: ₹ per kilometre. Replace with provider call later. */
    private static final double FARE_PER_KM = 12.0;

    /** Average urban driving speed for ETA estimation. */
    private static final double DRIVE_SPEED_KMH = 25.0;

    private final PbRideRepository rideRepository;
    private final GeoService geoService;

    @Override
    @Transactional
    @AuditAction("RIDE_CREATE")
    public RideResponse createFromInvitation(PbRideInvitationEntity inv) {
        // Run the Overlap Rule before persisting. If routes diverge, the
        // ride cannot be created and the invitation should remain UNRESOLVED
        // upstream — the caller decides what to do.
        RouteCompatibilityResponse compat = geoService.computeCompatibility(
                inv.getSenderLat(), inv.getSenderLng(),
                inv.getSenderDestLat(), inv.getSenderDestLng(),
                inv.getReceiverLat(), inv.getReceiverLng(),
                inv.getReceiverDestLat(), inv.getReceiverDestLng()
        );

        if (compat.getCompatibility() != com.akash.pooler_backend.enums.RouteCompatibility.COMPATIBLE) {
            throw new IncompatibleRouteException(
                    "Routes are " + compat.getCompatibility() + " ("
                            + compat.getBearingDeltaDegrees() + "° apart, "
                            + compat.getDetourPercent() + "% detour)");
        }

        boolean senderIsPrimary = "A".equals(compat.getPrimaryRoute());
        String primary   = senderIsPrimary ? inv.getSenderEntityId()   : inv.getReceiverEntityId();
        String secondary = senderIsPrimary ? inv.getReceiverEntityId() : inv.getSenderEntityId();

        double primaryDestLat   = senderIsPrimary ? inv.getSenderDestLat() : inv.getReceiverDestLat();
        double primaryDestLng   = senderIsPrimary ? inv.getSenderDestLng() : inv.getReceiverDestLng();
        String primaryDestAddr  = senderIsPrimary ? inv.getSenderDestAddress() : inv.getReceiverDestAddress();

        double secondaryDestLat = senderIsPrimary ? inv.getReceiverDestLat() : inv.getSenderDestLat();
        double secondaryDestLng = senderIsPrimary ? inv.getReceiverDestLng() : inv.getSenderDestLng();
        String secondaryDestAddr= senderIsPrimary ? inv.getReceiverDestAddress() : inv.getSenderDestAddress();

        // Cab path: pickup -> first drop (secondary) -> final drop (primary)
        double leg1 = GeoUtil.haversineKm(
                inv.getPickupLat(), inv.getPickupLng(),
                secondaryDestLat,   secondaryDestLng);
        double leg2 = GeoUtil.haversineKm(
                secondaryDestLat, secondaryDestLng,
                primaryDestLat,   primaryDestLng);
        double total = leg1 + leg2;

        PbRideEntity ride = PbRideEntity.builder()
                .entityId(newId())
                .invitationEntityId(inv.getEntityId())
                .primaryEntityId(primary)
                .secondaryEntityId(secondary)
                .pickupLat(inv.getPickupLat())
                .pickupLng(inv.getPickupLng())
                .pickupAddress(inv.getPickupAddress())
                .firstDropLat(secondaryDestLat).firstDropLng(secondaryDestLng)
                .firstDropAddress(secondaryDestAddr)
                .finalDropLat(primaryDestLat).finalDropLng(primaryDestLng)
                .finalDropAddress(primaryDestAddr)
                .totalDistanceKm(round(total, 4))
                .estimatedDurationMinutes((int) Math.round((total / DRIVE_SPEED_KMH) * 60.0))
                .estimatedFare(round(total * FARE_PER_KM, 2))
                .compatibility(compat.getCompatibility())
                .status(RideStatus.MATCHED)
                .build();

        ride = rideRepository.save(ride);
        log.info("Ride {} created from invitation {}: primary={}, secondary={}, totalKm={}",
                ride.getEntityId(), inv.getEntityId(), primary, secondary, total);
        return RideResponse.from(ride);
    }

    @Override
    @Transactional(readOnly = true)
    public RideResponse get(PbUserEntity user, String rideEntityId) {
        return RideResponse.from(loadParticipant(user, rideEntityId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponse> history(PbUserEntity user) {
        return rideRepository.findHistoryForUser(user.getEntityId())
                .stream().map(RideResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponse> active(PbUserEntity user) {
        return rideRepository.findActiveForUser(
                        user.getEntityId(),
                        List.of(RideStatus.COMPLETED, RideStatus.CANCELLED))
                .stream().map(RideResponse::from).toList();
    }

    @Override
    @Transactional
    @AuditAction("RIDE_STATUS_UPDATE")
    public RideResponse updateStatus(PbUserEntity user, String rideEntityId, UpdateRideStatusRequest req) {
        PbRideEntity ride = loadParticipant(user, rideEntityId);
        if (ride.getStatus().isTerminal()) {
            throw new RideInvalidStateException("Ride is already " + ride.getStatus());
        }
        ride.setStatus(req.getStatus());
        switch (req.getStatus()) {
            case IN_TRANSIT -> ride.setStartedAt(Instant.now());
            case COMPLETED  -> ride.setCompletedAt(Instant.now());
            case CANCELLED  -> ride.setCancelledAt(Instant.now());
            default -> { /* no timestamp side-effect */ }
        }
        return RideResponse.from(rideRepository.save(ride));
    }

    @Override
    @Transactional
    @AuditAction("RIDE_CANCEL")
    public RideResponse cancel(PbUserEntity user, String rideEntityId, CancelRideRequest req) {
        PbRideEntity ride = loadParticipant(user, rideEntityId);
        if (ride.getStatus().isTerminal()) {
            throw new RideInvalidStateException("Ride is already " + ride.getStatus());
        }
        ride.setStatus(RideStatus.CANCELLED);
        ride.setCancelledAt(Instant.now());
        if (req != null) ride.setCancelReason(req.getReason());
        return RideResponse.from(rideRepository.save(ride));
    }

    @Override
    @Transactional
    @AuditAction("RIDER_ARRIVAL_CONFIRM")
    public ArrivalConfirmationResponse confirmArrival(PbUserEntity user, String rideEntityId) {
        PbRideEntity ride = loadParticipant(user, rideEntityId);
        if (ride.getStatus().isTerminal()) {
            throw new RideInvalidStateException("Ride is already " + ride.getStatus());
        }
        if (ride.getPrimaryEntityId().equals(user.getEntityId())) ride.setPrimaryArrived(true);
        else ride.setSecondaryArrived(true);
        if (ride.bothArrived()) ride.setStatus(RideStatus.AT_PICKUP);
        ride = rideRepository.save(ride);
        return ArrivalConfirmationResponse.builder()
                .ride(RideResponse.from(ride))
                .bothArrived(ride.bothArrived())
                .build();
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private PbRideEntity loadParticipant(PbUserEntity user, String rideEntityId) {
        PbRideEntity ride = rideRepository.findByEntityId(rideEntityId)
                .orElseThrow(RideNotFoundException::new);
        if (!ride.isParticipant(user.getEntityId())) {
            throw new RideForbiddenException();
        }
        return ride;
    }

    private static double round(double v, int d) {
        double f = Math.pow(10, d);
        return Math.round(v * f) / f;
    }

    private static String newId() {
        return "rid-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
