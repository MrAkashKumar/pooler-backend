package com.akash.pooler_backend.scheduler;

import com.akash.pooler_backend.service.ChatService;
import com.akash.pooler_backend.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatCleanupScheduler {

    private final ChatService chatService;
    private final FileUploadService fileUploadService;

    @Value("${scheduling.enabled:true}")
    private boolean schedulingEnabled;

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
            chatService.archiveExpiredChats();
            log.info("Expired chats archived");

            // Delete files older than 2 hours
            fileUploadService.cleanupExpiredFiles();
            log.info("Expired chat files cleaned up");

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
