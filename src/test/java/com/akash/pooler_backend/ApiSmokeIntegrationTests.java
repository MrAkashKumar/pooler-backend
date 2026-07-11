package com.akash.pooler_backend;

import com.akash.pooler_backend.dto.request.DiscoveryToggleRequest;
import com.akash.pooler_backend.dto.request.CreateSafetyReportRequest;
import com.akash.pooler_backend.dto.request.UpdateFareSplitRequest;
import com.akash.pooler_backend.entity.PbRideEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.akash.pooler_backend.dto.request.SendMessageRequest;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.enums.ChatThreadStatus;
import com.akash.pooler_backend.enums.DiscoveryMode;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import com.akash.pooler_backend.repository.PbChatArchiveRepository;
import com.akash.pooler_backend.repository.PbChatThreadRepository;
import com.akash.pooler_backend.repository.PbRideInvitationRepository;
import com.akash.pooler_backend.repository.PbRideRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.service.RideService;
import com.akash.pooler_backend.service.SafetyReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired private PbUserRepository userRepository;
    @Autowired private PbRideInvitationRepository invitationRepository;
    @Autowired private PbRideRepository rideRepository;
    @Autowired private PbChatThreadRepository threadRepository;
    @Autowired private PbChatArchiveRepository archiveRepository;
    @Autowired private ChatService chatService;
    @Autowired private RideService rideService;
    @Autowired private SafetyReportService safetyReportService;

    @Test
    void publicHealthUsesApiEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void expoWebPreflightCanReachLoginEndpoint() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:8081")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers",
                                "content-type,x-platform,x-app-version,x-device-id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:8081"));
    }

    @Test
    void discoveryToggleAcceptsLegacyVisibleMode() throws Exception {
        DiscoveryToggleRequest request = objectMapper.readValue("""
                {
                  "mode": "VISIBLE",
                  "currentLatitude": 1.3152,
                  "currentLongitude": 103.7651
                }
                """, DiscoveryToggleRequest.class);

        assertEquals(DiscoveryMode.ON, request.getMode());
    }

    @Test
    void seededMobileLoginReturnsAllThreeTokenTypes() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Device-Id", "integration-test-device")
                        .header("X-Platform", "ANDROID")
                        .header("X-App-Version", "1.0.0")
                        .content("""
                                {
                                  "email": "akash@pooler.com",
                                  "password": "akash@123!",
                                  "platform": "ANDROID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.sessionToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value("akash@pooler.com"));
    }

    @Test
    void acceptedInvitationChatCanSendAndArchiveMessages() {
        var akash = userRepository.findByEmail("akash@pooler.com").orElseThrow();
        var alice = userRepository.findByEmail("alice@pooler.com").orElseThrow();
        String invitationId = "test-inv-" + UUID.randomUUID().toString().substring(0, 8);
        var invitation = invitationRepository.save(PbRideInvitationEntity.builder()
                .entityId(invitationId)
                .senderEntityId(akash.getEntityId())
                .receiverEntityId(alice.getEntityId())
                .senderLat(1.3152).senderLng(103.7651)
                .senderDestLat(1.2834).senderDestLng(103.8607)
                .receiverLat(1.3008).receiverLng(103.8565)
                .receiverDestLat(1.2834).receiverDestLng(103.8607)
                .status(InvitationStatusEnums.ACCEPTED)
                .expiresAt(Instant.now().plusSeconds(300))
                .build());

        var thread = chatService.createChatThread(akash, invitation);
        chatService.sendMessage(akash, thread.getEntityId(), SendMessageRequest.builder()
                .content("I am near the blue sign")
                .messageType("TEXT")
                .build());

        var messages = chatService.getMessages(thread.getEntityId(), 0, alice, PageRequest.of(0, 10));
        org.junit.jupiter.api.Assertions.assertEquals(1, messages.getTotalElements());

        thread.setExpiresAt(Instant.now().minusSeconds(1));
        threadRepository.save(thread);
        chatService.archiveExpiredChats();
        org.junit.jupiter.api.Assertions.assertEquals(ChatThreadStatus.ARCHIVED,
                threadRepository.findByEntityId(thread.getEntityId()).orElseThrow().getStatus());
        org.junit.jupiter.api.Assertions.assertTrue(archiveRepository.findByThreadId(thread.getEntityId()).isPresent());
    }

    @Test
    void fareSplitUsesDistanceWeightedShares() {
        var akash = userRepository.findByEmail("akash@pooler.com").orElseThrow();
        var alice = userRepository.findByEmail("alice@pooler.com").orElseThrow();
        String rideId = "ride-test-" + UUID.randomUUID().toString().substring(0, 8);
        rideRepository.save(PbRideEntity.builder()
                .entityId(rideId)
                .primaryEntityId(alice.getEntityId())
                .secondaryEntityId(akash.getEntityId())
                .pickupLat(1.3008).pickupLng(103.8565)
                .pickupAddress("Bugis MRT Exit B")
                .firstDropLat(1.3152).firstDropLng(103.7651)
                .firstDropAddress("Clementi")
                .finalDropLat(1.2834).finalDropLng(103.8607)
                .finalDropAddress("Marina Bay Sands")
                .primaryTripDistanceKm(15.0)
                .secondaryTripDistanceKm(10.0)
                .totalDistanceKm(15.0)
                .estimatedFare(30.0)
                .build());

        var response = rideService.updateFareSplit(akash, rideId,
                new UpdateFareSplitRequest(30.0, "SGD", "Grab"));

        assertEquals(18.0, response.getPrimaryFareShare());
        assertEquals(12.0, response.getSecondaryFareShare());
        assertEquals("Grab", response.getFareSplitProvider());
    }

    @Test
    void safetyReportCreateAndReadForReporter() {
        var akash = userRepository.findByEmail("akash@pooler.com").orElseThrow();

        var created = safetyReportService.create(akash, CreateSafetyReportRequest.builder()
                .rideEntityId("ride-safety-test")
                .category("Unsafe meetup place")
                .details("The pickup point was too isolated at night.")
                .contactAllowed(true)
                .latitude(1.3008)
                .longitude(103.8565)
                .build());

        var reports = safetyReportService.listForReporter(akash);

        org.junit.jupiter.api.Assertions.assertTrue(reports.stream()
                .anyMatch(report -> report.getEntityId().equals(created.getEntityId())));
        assertEquals("OPEN", created.getStatus().name());
    }
}
