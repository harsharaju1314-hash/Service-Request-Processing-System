package com.srps.dto;

import com.srps.enums.EventType;
import com.srps.enums.RequestStatus;
import java.time.LocalDateTime;

public class ServiceRequestEventDto {

    private String requestNumber;
    private EventType eventType;
    private RequestStatus status;
    private LocalDateTime timestamp;

    public ServiceRequestEventDto() {
    }

    public ServiceRequestEventDto(String requestNumber, EventType eventType, RequestStatus status, LocalDateTime timestamp) {
        this.requestNumber = requestNumber;
        this.eventType = eventType;
        this.status = status;
        this.timestamp = timestamp;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ServiceRequestEventDto{" +
                "requestNumber='" + requestNumber + '\'' +
                ", eventType=" + eventType +
                ", status=" + status +
                ", timestamp=" + timestamp +
                '}';
    }
}
