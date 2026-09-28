package com.srps.controller;

import com.srps.dto.AssignRequestDto;
import com.srps.dto.CreateServiceRequestDto;
import com.srps.dto.ServiceRequestResponseDto;
import com.srps.dto.UpdateRequestStatusDto;
import com.srps.enums.Priority;
import com.srps.enums.RequestStatus;
import com.srps.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class ServiceRequestController {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestController.class);

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    @PostMapping
    public ResponseEntity<ServiceRequestResponseDto> createRequest(@Valid @RequestBody CreateServiceRequestDto dto) {
        log.info("REST request to create service request: {}", dto.getRequestNumber());
        ServiceRequestResponseDto response = serviceRequestService.createRequest(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRequestResponseDto> getRequestById(@PathVariable Long id) {
        log.info("REST request to get service request id: {}", id);
        ServiceRequestResponseDto response = serviceRequestService.getRequestById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ServiceRequestResponseDto>> getRequests(
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) String category) {
        log.info("REST request to get service requests with filter [status={}, priority={}, category={}]",
                status, priority, category);
        List<ServiceRequestResponseDto> list = serviceRequestService.getRequests(status, priority, category);
        return ResponseEntity.ok(list);
    }

    @PutMapping("/{id}/assign")
    public ResponseEntity<ServiceRequestResponseDto> assignRequest(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequestDto dto) {
        log.info("REST request to assign service request id: {} to: {}", id, dto.getAssignedTo());
        ServiceRequestResponseDto response = serviceRequestService.assignRequest(id, dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ServiceRequestResponseDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequestStatusDto dto) {
        log.info("REST request to update status for service request id: {} to: {}", id, dto.getStatus());
        ServiceRequestResponseDto response = serviceRequestService.updateStatus(id, dto);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<ServiceRequestResponseDto> closeRequest(@PathVariable Long id) {
        log.info("REST request to close service request id: {}", id);
        ServiceRequestResponseDto response = serviceRequestService.closeRequest(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ServiceRequestResponseDto> cancelRequest(@PathVariable Long id) {
        log.info("REST request to cancel service request id: {}", id);
        ServiceRequestResponseDto response = serviceRequestService.cancelRequest(id);
        return ResponseEntity.ok(response);
    }
}
