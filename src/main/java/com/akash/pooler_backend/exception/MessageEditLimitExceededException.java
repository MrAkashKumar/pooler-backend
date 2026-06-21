package com.akash.pooler_backend.exception;

import com.akash.pooler_backend.enums.ErrorCode;

public class MessageEditLimitExceededException extends BaseException {

    public MessageEditLimitExceededException() {
        super(ErrorCode.MESSAGE_EDIT_LIMIT_EXCEEDED, "Messages can only be edited within 15 minutes of sending");
    }

    public MessageEditLimitExceededException(String message) {
        super(ErrorCode.MESSAGE_EDIT_LIMIT_EXCEEDED, message);
    }
}