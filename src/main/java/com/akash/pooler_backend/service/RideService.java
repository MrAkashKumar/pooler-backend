package com.akash.pooler_backend.service;

import com.akash.pooler_backend.dto.request.CancelRideRequest;
import com.akash.pooler_backend.dto.request.UpdateRideStatusRequest;
import com.akash.pooler_backend.dto.response.RideResponse;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.entity.PbUserEntity;

import java.util.List;

/**
 * Manages the lifecycle of confirmed shared rides.
 *
 * @author Akash Kumar
 */
public interface RideService {

    /**
     * Create a ride from a fully-confirmed invitation. Called internally
     * when both sender and receiver have confirmed the pickup hub.
     */
    RideResponse createFromInvitation(PbRideInvitationEntity invitation);

    RideResponse get(PbUserEntity user, String rideEntityId);

    List<RideResponse> history(PbUserEntity user);

    List<RideResponse> active(PbUserEntity user);

    RideResponse updateStatus(PbUserEntity user, String rideEntityId, UpdateRideStatusRequest req);

    RideResponse cancel(PbUserEntity user, String rideEntityId, CancelRideRequest req);
}
