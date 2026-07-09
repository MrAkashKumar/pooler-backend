package com.akash.pooler_backend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareTelegramRequest {
    private String telegramHandle;
    private String telegramPhoneNumber;
}
