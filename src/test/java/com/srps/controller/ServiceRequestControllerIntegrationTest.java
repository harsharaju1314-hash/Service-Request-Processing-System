package com.srps.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srps.dto.AssignRequestDto;
import com.srps.dto.CreateServiceRequestDto;
import com.srps.dto.ServiceRequestResponseDto;
import com.srps.dto.UpdateRequestStatusDto;
import com.srps.enums.Priority;
import com.srps.enums.RequestStatus;
import com.srps.exception.InvalidStatusTransitionException;
import com.srps.exception.ServiceRequestNotFoundException;
import com.srps.service.ServiceRequestService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ServiceRequestController.class)
class ServiceRequestControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ServiceRequestService serviceRequestService;

    private ServiceRequestResponseDto createSampleResponse(RequestStatus status) {
        return new ServiceRequestResponseDto(
                10L,
                "SR-1001",
                1L,
                "Ravi Kumar",
                "ravi.kumar@example.com",
                "Account Access",
                "Unable to access account",
                Priority.HIGH,
                status,
                "agent_john",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("POST /api/requests - Should create service request and return 201 Created")
    void shouldCreateServiceRequest() throws Exception {
        CreateServiceRequestDto dto = new CreateServiceRequestDto("SR-1001", 1L, "Account Access",
                "Unable to access account", Priority.HIGH);
        ServiceRequestResponseDto responseDto = createSampleResponse(RequestStatus.OPEN);

        when(serviceRequestService.createRequest(any(CreateServiceRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.requestNumber").value("SR-1001"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /api/requests - Should return 400 Bad Request when validation fails")
    void shouldReturnBadRequestOnInvalidPayload() throws Exception {
        CreateServiceRequestDto invalidDto = new CreateServiceRequestDto("", null, "", "", null);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.requestNumber").exists())
                .andExpect(jsonPath("$.validationErrors.customerId").exists())
                .andExpect(jsonPath("$.validationErrors.category").exists())
                .andExpect(jsonPath("$.validationErrors.description").exists())
                .andExpect(jsonPath("$.validationErrors.priority").exists());
    }

    @Test
    @DisplayName("GET /api/requests/{id} - Should return request when ID exists")
    void shouldGetRequestById() throws Exception {
        ServiceRequestResponseDto responseDto = createSampleResponse(RequestStatus.OPEN);
        when(serviceRequestService.getRequestById(10L)).thenReturn(responseDto);

        mockMvc.perform(get("/api/requests/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestNumber").value("SR-1001"));
    }

    @Test
    @DisplayName("GET /api/requests/{id} - Should return 404 when request does not exist")
    void shouldReturnNotFoundForMissingRequest() throws Exception {
        when(serviceRequestService.getRequestById(999L))
                .thenThrow(new ServiceRequestNotFoundException("Service request not found with id: 999"));

        mockMvc.perform(get("/api/requests/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Service request not found with id: 999"));
    }

    @Test
    @DisplayName("GET /api/requests - Should return filtered list of requests")
    void shouldFilterRequests() throws Exception {
        ServiceRequestResponseDto responseDto = createSampleResponse(RequestStatus.OPEN);
        when(serviceRequestService.getRequests(RequestStatus.OPEN, Priority.HIGH, "Account Access"))
                .thenReturn(List.of(responseDto));

        mockMvc.perform(get("/api/requests")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("category", "Account Access"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].requestNumber").value("SR-1001"));
    }

    @Test
    @DisplayName("PUT /api/requests/{id}/assign - Should assign request")
    void shouldAssignRequest() throws Exception {
        AssignRequestDto dto = new AssignRequestDto("agent_sarah");
        ServiceRequestResponseDto responseDto = createSampleResponse(RequestStatus.ASSIGNED);

        when(serviceRequestService.assignRequest(eq(10L), any(AssignRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/requests/10/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"));
    }

    @Test
    @DisplayName("PUT /api/requests/{id}/status - Should update status")
    void shouldUpdateStatus() throws Exception {
        UpdateRequestStatusDto dto = new UpdateRequestStatusDto(RequestStatus.IN_PROGRESS);
        ServiceRequestResponseDto responseDto = createSampleResponse(RequestStatus.IN_PROGRESS);

        when(serviceRequestService.updateStatus(eq(10L), any(UpdateRequestStatusDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/requests/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PUT /api/requests/{id}/status - Should return 409 Conflict on invalid status transition")
    void shouldReturnConflictOnInvalidStatusTransition() throws Exception {
        UpdateRequestStatusDto dto = new UpdateRequestStatusDto(RequestStatus.RESOLVED);

        when(serviceRequestService.updateStatus(eq(10L), any(UpdateRequestStatusDto.class)))
                .thenThrow(new InvalidStatusTransitionException("Request SR-1001 cannot move from OPEN to RESOLVED"));

        mockMvc.perform(put("/api/requests/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Request SR-1001 cannot move from OPEN to RESOLVED"));
    }

    @Test
    @DisplayName("PUT /api/requests/{id}/close - Should close request")
    void shouldCloseRequest() throws Exception {
        ServiceRequestResponseDto responseDto = createSampleResponse(RequestStatus.CLOSED);
        when(serviceRequestService.closeRequest(10L)).thenReturn(responseDto);

        mockMvc.perform(put("/api/requests/10/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    @DisplayName("PUT /api/requests/{id}/cancel - Should cancel request")
    void shouldCancelRequest() throws Exception {
        ServiceRequestResponseDto responseDto = createSampleResponse(RequestStatus.CANCELLED);
        when(serviceRequestService.cancelRequest(10L)).thenReturn(responseDto);

        mockMvc.perform(put("/api/requests/10/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
