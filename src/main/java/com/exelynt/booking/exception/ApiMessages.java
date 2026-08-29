package com.exelynt.booking.exception;

public final class ApiMessages {
    public static final String ACCESS_OWN_RESERVATIONS_ONLY = "You can access only your own reservations";
    public static final String ADMIN_DELETE_RESERVATIONS_ONLY = "Only admins can delete reservations";
    public static final String ADMIN_UPDATE_RESERVATIONS_ONLY = "Only admins can update reservations";
    public static final String ADMIN_UPDATE_RESERVATION_STATUS_ONLY = "Only admins can update reservation status";
    public static final String INVALID_REQUEST_VALUE_OR_JSON = "Invalid request value or JSON body";
    public static final String INVALID_OR_EXPIRED_JWT = "Invalid or expired JWT token";
    public static final String RESOURCE_NOT_AVAILABLE = "Resource is not available for booking";
    public static final String RESERVATION_END_AFTER_START = "Reservation endTime must be after startTime";
    public static final String RESERVATION_NOT_FOUND = "Reservation not found";
    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String UNEXPECTED_SERVER_ERROR = "Unexpected server error";
    public static final String USER_NOT_FOUND = "User not found";
    public static final String VALIDATION_FAILED = "Validation failed";

    private ApiMessages() {
    }
}
