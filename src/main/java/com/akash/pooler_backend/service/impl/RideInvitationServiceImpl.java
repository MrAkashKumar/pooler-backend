package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.dto.request.AcceptInvitationRequest;
import com.akash.pooler_backend.dto.request.SendRideInvitationRequest;
import com.akash.pooler_backend.dto.response.CommonPickupPointResponse;
import com.akash.pooler_backend.dto.response.RideInvitationResponse;
import com.akash.pooler_backend.dto.response.RideResponse;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import com.akash.pooler_backend.exception.InvitationExpiredException;
import com.akash.pooler_backend.exception.InvitationForbiddenException;
import com.akash.pooler_backend.exception.InvitationInvalidStateException;
import com.akash.pooler_backend.exception.InvitationNotFoundException;
import com.akash.pooler_backend.exception.InvitationSelfNotAllowedException;
import com.akash.pooler_backend.exception.UserNotFoundException;
import com.akash.pooler_backend.interceptors.annotation.AuditAction;
import com.akash.pooler_backend.repository.PbRideInvitationRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.GeoService;
import com.akash.pooler_backend.service.RideInvitationService;
import com.akash.pooler_backend.service.RideService;
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
public class RideInvitationServiceImpl implements RideInvitationService {

    private static final int DEFAULT_TTL_SECONDS = 300; // 5 minutes

    private final PbRideInvitationRepository invitationRepository;
    private final PbUserRepository userRepository;
    private final GeoService geoService;
    private final RideService rideService;

    @Override
    @Transactional
    @AuditAction("INVITATION_SEND")
    public RideInvitationResponse send(PbUserEntity sender, SendRideInvitationRequest req) {
        if (sender.getEntityId().equals(req.getReceiverEntityId())) {
            throw new InvitationSelfNotAllowedException();
        }
        userRepository.findByEntityId(req.getReceiverEntityId())
                .orElseThrow(() -> new UserNotFoundException(req.getReceiverEntityId()));

        int ttl = req.getTtlSeconds() != null ? req.getTtlSeconds() : DEFAULT_TTL_SECONDS;

        PbRideInvitationEntity entity = PbRideInvitationEntity.builder()
                .entityId(newId())
                .senderEntityId(sender.getEntityId())
                .receiverEntityId(req.getReceiverEntityId())
                .senderLat(req.getSenderLatitude())
                .senderLng(req.getSenderLongitude())
                .senderDestLat(req.getSenderDestinationLatitude())
                .senderDestLng(req.getSenderDestinationLongitude())
                .senderDestAddress(req.getSenderDestinationAddress())
                .status(InvitationStatusEnums.PENDING)
                .expiresAt(Instant.now().plusSeconds(ttl))
                .message(req.getMessage())
                .build();

        entity = invitationRepository.save(entity);
        log.info("Invitation {} sent from {} to {}",
                entity.getEntityId(), sender.getEntityId(), req.getReceiverEntityId());
        return RideInvitationResponse.from(entity);
    }

    @Override
    @Transactional
    @AuditAction("INVITATION_ACCEPT")
    public RideInvitationResponse accept(PbUserEntity receiver, String invitationEntityId, AcceptInvitationRequest req) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);

        if (!inv.getReceiverEntityId().equals(receiver.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        ensurePendingAndFresh(inv);

        inv.setReceiverLat(req.getReceiverLatitude());
        inv.setReceiverLng(req.getReceiverLongitude());
        inv.setReceiverDestLat(req.getReceiverDestinationLatitude());
        inv.setReceiverDestLng(req.getReceiverDestinationLongitude());
        inv.setReceiverDestAddress(req.getReceiverDestinationAddress());

        // Run the Meet-in-the-Middle engine for the pickup hub
        CommonPickupPointResponse pickup = geoService.computeCommonPickup(
                inv.getSenderLat(), inv.getSenderLng(),
                inv.getReceiverLat(), inv.getReceiverLng()
        );
        inv.setPickupLat(pickup.getPickupLatitude());
        inv.setPickupLng(pickup.getPickupLongitude());
        inv.setPickupAddress(pickup.getPickupAddress());
        inv.setEstimatedWalkDistanceKm(
                Math.max(pickup.getDistanceFromUserAKm(), pickup.getDistanceFromUserBKm()));

        inv.setStatus(InvitationStatusEnums.ACCEPTED);
        inv.setRespondedAt(Instant.now());

        inv = invitationRepository.save(inv);
        log.info("Invitation {} accepted; pickup hub computed at ({}, {})",
                invitationEntityId, inv.getPickupLat(), inv.getPickupLng());
        return RideInvitationResponse.from(inv);
    }

    @Override
    @Transactional
    @AuditAction("INVITATION_DECLINE")
    public RideInvitationResponse decline(PbUserEntity receiver, String invitationEntityId) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);
        if (!inv.getReceiverEntityId().equals(receiver.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        ensurePendingAndFresh(inv);
        inv.setStatus(InvitationStatusEnums.DECLINED);
        inv.setRespondedAt(Instant.now());
        return RideInvitationResponse.from(invitationRepository.save(inv));
    }

    @Override
    @Transactional
    @AuditAction("INVITATION_CONFIRM_PICKUP")
    public ConfirmResult confirmPickup(PbUserEntity user, String invitationEntityId) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);
        if (!inv.isParticipant(user.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        if (inv.getStatus() != InvitationStatusEnums.ACCEPTED) {
            throw new InvitationInvalidStateException(
                    "Invitation must be ACCEPTED before confirming pickup");
        }

        if (inv.getSenderEntityId().equals(user.getEntityId())) {
            inv.setSenderConfirmed(true);
        } else {
            inv.setReceiverConfirmed(true);
        }
        inv = invitationRepository.save(inv);

        // Promote to Ride when both have confirmed
        if (inv.isFullyConfirmed()) {
            RideResponse ride = rideService.createFromInvitation(inv);
            log.info("Both parties confirmed invitation {} — ride {} created",
                    invitationEntityId, ride.getEntityId());
            return new ConfirmResult(RideInvitationResponse.from(inv), ride);
        }

        return new ConfirmResult(RideInvitationResponse.from(inv), null);
    }

    @Override
    @Transactional
    @AuditAction("INVITATION_CANCEL")
    public RideInvitationResponse cancel(PbUserEntity user, String invitationEntityId) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);
        if (!inv.isParticipant(user.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        if (inv.getStatus() == InvitationStatusEnums.DECLINED) {
            throw new InvitationInvalidStateException("Invitation already declined");
        }
        inv.setStatus(InvitationStatusEnums.DECLINED);
        inv.setRespondedAt(Instant.now());
        inv.setMessage((inv.getMessage() == null ? "" : inv.getMessage() + " | ")
                + "Cancelled by " + user.getEntityId());
        return RideInvitationResponse.from(invitationRepository.save(inv));
    }

    @Override
    @Transactional(readOnly = true)
    public RideInvitationResponse get(PbUserEntity user, String invitationEntityId) {
        PbRideInvitationEntity inv = invitationRepository.findByEntityId(invitationEntityId)
                .orElseThrow(InvitationNotFoundException::new);
        if (!inv.isParticipant(user.getEntityId())) {
            throw new InvitationForbiddenException();
        }
        return RideInvitationResponse.from(inv);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideInvitationResponse> inbox(PbUserEntity user) {
        return invitationRepository
                .findAllByReceiverEntityIdOrderByCreatedAtDesc(user.getEntityId())
                .stream().map(RideInvitationResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideInvitationResponse> outbox(PbUserEntity user) {
        return invitationRepository
                .findAllBySenderEntityIdOrderByCreatedAtDesc(user.getEntityId())
                .stream().map(RideInvitationResponse::from).toList();
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private void ensurePendingAndFresh(PbRideInvitationEntity inv) {
        if (inv.getStatus() != InvitationStatusEnums.PENDING) {
            throw new InvitationInvalidStateException(
                    "Invitation is " + inv.getStatus() + " and cannot be modified");
        }
        if (inv.isExpired()) {
            inv.setStatus(InvitationStatusEnums.DECLINED);
            invitationRepository.save(inv);
            throw new InvitationExpiredException();
        }
    }

    private static String newId() {
        return "inv-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
}
