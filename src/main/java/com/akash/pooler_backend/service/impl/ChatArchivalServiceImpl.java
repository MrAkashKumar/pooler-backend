package com.akash.pooler_backend.service.impl;

import com.akash.pooler_backend.entity.PbChatArchiveEntity;
import com.akash.pooler_backend.entity.PbChatMessageEntity;
import com.akash.pooler_backend.entity.PbChatThreadEntity;
import com.akash.pooler_backend.exception.ChatNotFoundException;
import com.akash.pooler_backend.repository.PbChatArchiveRepository;
import com.akash.pooler_backend.repository.PbChatMessageRepository;
import com.akash.pooler_backend.repository.PbChatThreadRepository;
import com.akash.pooler_backend.service.ChatArchivalService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatArchivalServiceImpl implements ChatArchivalService {

    private final PbChatArchiveRepository archiveRepository;
    private final PbChatThreadRepository threadRepository;
    private final PbChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void archiveThread(String threadId) {
        if (archiveRepository.findByThreadId(threadId).isPresent()) return;
        PbChatThreadEntity thread = threadRepository.findByEntityId(threadId).orElseThrow(ChatNotFoundException::new);
        List<PbChatMessageEntity> messages = messageRepository.findByThreadAndCreatedAfter(threadId, Instant.EPOCH);
        Map<String, Object> archive = Map.of(
                "thread", Map.of(
                        "entityId", thread.getEntityId(),
                        "invitationEntityId", thread.getInvitationEntityId(),
                        "participant1UserId", thread.getParticipant1UserId(),
                        "participant2UserId", thread.getParticipant2UserId(),
                        "expiresAt", thread.getExpiresAt().toString()),
                "messages", messages.stream().map(message -> Map.of(
                        "entityId", message.getEntityId(),
                        "senderEntityId", message.getSender(),
                        "content", message.getContent(),
                        "type", message.getMessageType().name(),
                        "createdAt", message.getCreatedAt().toString())).toList());
        try {
            byte[] serialized = objectMapper.writeValueAsBytes(archive);
            archiveRepository.save(PbChatArchiveEntity.builder()
                    .entityId("archive-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20))
                    .threadId(threadId)
                    .archiveData(archive)
                    .sizeBytes((long) serialized.length)
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not archive chat " + threadId, exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String retrieveArchivedThread(String threadId) {
        PbChatArchiveEntity archive = archiveRepository.findByThreadId(threadId).orElseThrow(ChatNotFoundException::new);
        try { return objectMapper.writeValueAsString(archive.getArchiveData()); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Could not read chat archive", exception); }
    }

    @Override
    @Transactional(readOnly = true)
    public long getArchivedMessageCount(String threadId) {
        return archiveRepository.findByThreadId(threadId).isPresent() ? messageRepository.countByThreadId(threadId) : 0;
    }
}
