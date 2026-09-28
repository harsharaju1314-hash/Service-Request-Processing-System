package com.srps.dto;

import com.srps.enums.RequestStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateRequestStatusDto {

    @NotNull(message = "Status is required")
    private RequestStatus status;

    public UpdateRequestStatusDto() {
    }

    public UpdateRequestStatusDto(RequestStatus status) {
        this.status = status;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
