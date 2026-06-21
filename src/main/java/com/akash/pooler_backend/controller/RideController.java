package com.akash.pooler_backend.controller;

import com.akash.pooler_backend.dto.request.CancelRideRequest;
import com.akash.pooler_backend.dto.request.UpdateRideStatusRequest;
import com.akash.pooler_backend.dto.response.ApiResponse;
import com.akash.pooler_backend.dto.response.RideResponse;
import com.akash.pooler_backend.dto.response.ArrivalConfirmationResponse;
import com.akash.pooler_backend.entity.PbUserEntity;
import com.akash.pooler_backend.interceptors.annotation.CurrentUser;
import com.akash.pooler_backend.interceptors.annotation.ValidSession;
import com.akash.pooler_backend.service.RideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lifecycle of confirmed shared rides.
 *
 * @author Akash Kumar
 */
@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Rides", description = "Shared-ride lifecycle and history")
public class RideController {

    private final RideService rideService;

    @GetMapping("/{rideEntityId}")
    @Operation(summary = "Fetch a single ride (must be a participant)")
    public ResponseEntity<ApiResponse<RideResponse>> get(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.get(user, rideEntityId)));
    }

    @GetMapping("/active")
    @Operation(summary = "List rides currently in progress")
    public ResponseEntity<ApiResponse<List<RideResponse>>> active(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.active(user)));
    }

    @GetMapping("/history")
    @Operation(summary = "List completed and cancelled rides")
    public ResponseEntity<ApiResponse<List<RideResponse>>> history(@CurrentUser PbUserEntity user) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.history(user)));
    }

    @PutMapping("/{rideEntityId}/status")
    @ValidSession(reason = "Changing ride status requires an active session")
    @Operation(summary = "Advance a ride to the next lifecycle state")
    public ResponseEntity<ApiResponse<RideResponse>> updateStatus(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId,
            @Valid @RequestBody UpdateRideStatusRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Ride status updated",
                rideService.updateStatus(user, rideEntityId, req)));
    }

    @PostMapping("/{rideEntityId}/cancel")
    @ValidSession(reason = "Cancelling a ride requires an active session")
    @Operation(summary = "Cancel a non-terminal ride")
    public ResponseEntity<ApiResponse<RideResponse>> cancel(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId,
            @Valid @RequestBody(required = false) CancelRideRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Ride cancelled",
                rideService.cancel(user, rideEntityId, req)));
    }

    @PostMapping("/{rideEntityId}/arrive")
    @ValidSession(reason = "Arrival confirmation requires an active session")
    @Operation(summary = "Confirm physical arrival; cab handoff unlocks after both riders confirm")
    public ResponseEntity<ApiResponse<ArrivalConfirmationResponse>> arrive(
            @CurrentUser PbUserEntity user,
            @PathVariable String rideEntityId) {
        return ResponseEntity.ok(ApiResponse.ok(rideService.confirmArrival(user, rideEntityId)));
    }
}
