package com.starling.roundup.model.response;

import com.starling.roundup.entity.Status;

public enum StatusResponse {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    NOT_FOUND,
    ALREADY_IN_PROGRESS,
    ALREADY_COMPLETED;

    public static StatusResponse fromStatus(Status status) {
        return switch (status) {
            case IN_PROGRESS -> IN_PROGRESS;
            case COMPLETED -> COMPLETED;
            case FAILED -> FAILED;
        };
    }
}