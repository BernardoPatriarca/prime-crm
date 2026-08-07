package com.primecrm.shared.exception;

import org.springframework.http.HttpStatus;

public abstract class ApiException extends RuntimeException {

    private final String errorCode;
    private final HttpStatus httpStatus;

    protected ApiException(String errorCode, HttpStatus httpStatus, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
