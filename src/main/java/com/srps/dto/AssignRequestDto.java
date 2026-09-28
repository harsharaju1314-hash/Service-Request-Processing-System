package com.srps.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AssignRequestDto {

    @NotBlank(message = "Assigned agent / employee name is required")
    @Size(max = 100, message = "AssignedTo must not exceed 100 characters")
    private String assignedTo;

    public AssignRequestDto() {
    }

    public AssignRequestDto(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }
}
