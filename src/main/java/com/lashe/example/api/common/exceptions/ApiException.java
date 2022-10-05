package com.lashe.example.api.common.exceptions;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.http.HttpStatus;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1682235077860078063L;

    private final HttpStatus status;
    /*
     * Business code. For ID and for i18n purposes.
     */
    private final String code;
    private final String message;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.code = status.name();
        this.message = message;
    }

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
    }
}