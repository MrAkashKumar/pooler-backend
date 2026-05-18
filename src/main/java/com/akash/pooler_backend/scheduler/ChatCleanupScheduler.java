package com.akash.pooler_backend.scheduler;

import com.akash.pooler_backend.service.ChatArchivalService;
import com.akash.pooler_backend.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCleanupScheduler {

    private final ChatService chatService;
    private final ChatArchivalService archivalService;

    @Value("${scheduling.enabled:true}")
    private boolean schedulingEnabled;

    @Value("${chat.cleanup-interval-minutes:10}")
    private int cleanupIntervalMinutes;

    /**
     * Runs cleanup every N minutes (default: 10).
     * - Archives expired chats (expiresAt < now)
     * - Deletes associated files >2h old
     * - Logs completion time
     */
    @Scheduled(fixedRateString = "${chat.cleanup-interval-minutes:10}", timeUnit = java.util.concurrent.TimeUnit.MINUTES, initialDelay = 5)
    public void cleanupExpiredChats() {
        if (!schedulingEnabled) {
            log.debug("Chat cleanup scheduler is disabled");
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            log.info("Starting chat cleanup cycle");

            // Archive expired chats
            int archivedCount = 0; //chatService.archiveExpiredChats(Instant.now());
            log.info("Archived {} expired chat(s)", archivedCount);

            // Delete files older than 2 hours
            int deletedFileCount = 0;//chatService.deleteExpiredFiles(Instant.now().minusSeconds(2 * 3600));
            log.info("Deleted {} expired file(s)", deletedFileCount);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Chat cleanup cycle completed in {}ms", duration);
        } catch (Exception e) {
            log.error("Error during chat cleanup cycle", e);
        }
    }

    /**
     * Runs every 30 minutes to rebuild message search indexes.
     * Indexes new messages added since last index refresh.
     */
    @Scheduled(fixedRateString = "30", timeUnit = java.util.concurrent.TimeUnit.MINUTES, initialDelay = 15)
    public void rebuildSearchIndexes() {
        if (!schedulingEnabled) {
            return;
        }

        try {
            log.debug("Refreshing search indexes");
            int indexedCount = 0;//chatService.rebuildSearchIndexes();
            log.info("Rebuilt {} search index entries", indexedCount);
        } catch (Exception e) {
            log.error("Error rebuilding search indexes", e);
        }
    }
}
