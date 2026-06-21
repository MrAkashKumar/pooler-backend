package com.akash.pooler_backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.akash.pooler_backend.dto.request.SendMessageRequest;
import com.akash.pooler_backend.entity.PbRideInvitationEntity;
import com.akash.pooler_backend.enums.ChatThreadStatus;
import com.akash.pooler_backend.enums.InvitationStatusEnums;
import com.akash.pooler_backend.repository.PbChatArchiveRepository;
import com.akash.pooler_backend.repository.PbChatThreadRepository;
import com.akash.pooler_backend.repository.PbRideInvitationRepository;
import com.akash.pooler_backend.repository.PbUserRepository;
import com.akash.pooler_backend.service.ChatService;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSmokeIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired private PbUserRepository userRepository;
    @Autowired private PbRideInvitationRepository invitationRepository;
    @Autowired private PbChatThreadRepository threadRepository;
    @Autowired private PbChatArchiveRepository archiveRepository;
    @Autowired private ChatService chatService;

    @Test
    void publicHealthUsesApiEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
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
}
