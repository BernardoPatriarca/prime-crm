package com.primecrm.shared.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super("CONFLICT", HttpStatus.CONFLICT, message);
    }
}
