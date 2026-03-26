package com.findmyvibe.common.exception;

import java.util.UUID;

public class SessionNotFoundException extends RuntimeException {

    public SessionNotFoundException(UUID sessionId) {
        super("세션을 찾을 수 없습니다: " + sessionId);
    }
}
