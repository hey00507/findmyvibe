package com.findmyvibe.common.exception;

import com.findmyvibe.domain.enums.SessionStatus;

public class InvalidSessionStateException extends RuntimeException {

    public InvalidSessionStateException(SessionStatus current, SessionStatus required) {
        super("세션 상태가 올바르지 않습니다. 현재: " + current + ", 필요: " + required);
    }
}
