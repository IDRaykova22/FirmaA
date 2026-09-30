package com.firmaa.techdept.exceptions;

/** Wrong credentials, or the logged-in user no longer exists. Sent to the client as 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
