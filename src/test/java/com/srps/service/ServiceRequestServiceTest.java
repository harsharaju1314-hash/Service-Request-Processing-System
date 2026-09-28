package com.srps.service;

import com.srps.dto.AssignRequestDto;
import com.srps.dto.CreateServiceRequestDto;
import com.srps.dto.ServiceRequestResponseDto;
import com.srps.dto.UpdateRequestStatusDto;
import com.srps.entity.Customer;
import com.srps.entity.ServiceRequest;
import com.srps.enums.EventType;
import com.srps.enums.Priority;
import com.srps.enums.RequestStatus;
import com.srps.exception.CustomerNotFoundException;
import com.srps.exception.DuplicateRequestException;
import com.srps.exception.InvalidStatusTransitionException;
import com.srps.exception.ServiceRequestNotFoundException;
import com.srps.kafka.ServiceRequestEventProducer;
import com.srps.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestServiceTest {

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ServiceRequestEventProducer eventProducer;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    private Customer sampleCustomer;
    private ServiceRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer(1L, "Ravi Kumar", "ravi.kumar@example.com", LocalDateTime.now());
        sampleRequest = new ServiceRequest("SR-1001", sampleCustomer, "Account Access",
                "Unable to access account", Priority.HIGH, RequestStatus.OPEN);
        sampleRequest.setId(10L);
        sampleRequest.setCreatedAt(LocalDateTime.now());
        sampleRequest.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("1. Should create service request successfully and trigger Kafka event")
    void shouldCreateServiceRequestSuccessfully() {
        CreateServiceRequestDto dto = new CreateServiceRequestDto("SR-1001", 1L, "Account Access",
                "Unable to access account", Priority.HIGH);

        when(serviceRequestRepository.existsByRequestNumber("SR-1001")).thenReturn(false);
        when(customerService.getCustomerEntityById(1L)).thenReturn(sampleCustomer);
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenReturn(sampleRequest);

        ServiceRequestResponseDto response = serviceRequestService.createRequest(dto);

        assertNotNull(response);
        assertEquals("SR-1001", response.getRequestNumber());
        assertEquals(RequestStatus.OPEN, response.getStatus());
        assertEquals("Ravi Kumar", response.getCustomerName());

        verify(serviceRequestRepository).save(any(ServiceRequest.class));
        verify(eventProducer).publishEvent("SR-1001", EventType.REQUEST_CREATED, RequestStatus.OPEN);
    }

    @Test
    @DisplayName("2. Should throw CustomerNotFoundException when customer ID does not exist")
    void shouldThrowExceptionWhenCustomerNotFound() {
        CreateServiceRequestDto dto = new CreateServiceRequestDto("SR-1001", 999L, "Account Access",
                "Unable to access account", Priority.HIGH);

        when(serviceRequestRepository.existsByRequestNumber("SR-1001")).thenReturn(false);
        when(customerService.getCustomerEntityById(999L))
                .thenThrow(new CustomerNotFoundException("Customer not found with id: 999"));

        assertThrows(CustomerNotFoundException.class, () -> serviceRequestService.createRequest(dto));
        verify(serviceRequestRepository, never()).save(any(ServiceRequest.class));
        verify(eventProducer, never()).publishEvent(any(), any(), any());
    }

    @Test
    @DisplayName("3. Should throw DuplicateRequestException when request number already exists")
    void shouldThrowExceptionWhenDuplicateRequestNumber() {
        CreateServiceRequestDto dto = new CreateServiceRequestDto("SR-1001", 1L, "Account Access",
                "Unable to access account", Priority.HIGH);

        when(serviceRequestRepository.existsByRequestNumber("SR-1001")).thenReturn(true);

        assertThrows(DuplicateRequestException.class, () -> serviceRequestService.createRequest(dto));
        verify(serviceRequestRepository, never()).save(any(ServiceRequest.class));
        verify(eventProducer, never()).publishEvent(any(), any(), any());
    }

    @Test
    @DisplayName("4. Should assign request successfully and update status to ASSIGNED")
    void shouldAssignRequestSuccessfully() {
        AssignRequestDto dto = new AssignRequestDto("agent_sarah");

        when(serviceRequestRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(sampleRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponseDto response = serviceRequestService.assignRequest(10L, dto);

        assertNotNull(response);
        assertEquals("agent_sarah", response.getAssignedTo());
        assertEquals(RequestStatus.ASSIGNED, response.getStatus());

        verify(eventProducer).publishEvent("SR-1001", EventType.REQUEST_ASSIGNED, RequestStatus.ASSIGNED);
    }

    @Test
    @DisplayName("5. Should throw ServiceRequestNotFoundException when assigning non-existent request")
    void shouldThrowExceptionWhenAssigningNonExistentRequest() {
        AssignRequestDto dto = new AssignRequestDto("agent_sarah");
        when(serviceRequestRepository.findByIdWithCustomer(999L)).thenReturn(Optional.empty());

        assertThrows(ServiceRequestNotFoundException.class, () -> serviceRequestService.assignRequest(999L, dto));
        verify(serviceRequestRepository, never()).save(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("6. Should allow valid status transitions: ASSIGNED -> IN_PROGRESS -> RESOLVED -> CLOSED")
    void shouldAllowValidStatusTransitions() {
        // Step 1: ASSIGNED -> IN_PROGRESS
        sampleRequest.setStatus(RequestStatus.ASSIGNED);
        sampleRequest.setAssignedTo("agent_sarah");
        when(serviceRequestRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(sampleRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateRequestStatusDto inProgressDto = new UpdateRequestStatusDto(RequestStatus.IN_PROGRESS);
        ServiceRequestResponseDto inProgressResp = serviceRequestService.updateStatus(10L, inProgressDto);
        assertEquals(RequestStatus.IN_PROGRESS, inProgressResp.getStatus());
        verify(eventProducer).publishEvent("SR-1001", EventType.REQUEST_STARTED, RequestStatus.IN_PROGRESS);

        // Step 2: IN_PROGRESS -> RESOLVED
        UpdateRequestStatusDto resolvedDto = new UpdateRequestStatusDto(RequestStatus.RESOLVED);
        ServiceRequestResponseDto resolvedResp = serviceRequestService.updateStatus(10L, resolvedDto);
        assertEquals(RequestStatus.RESOLVED, resolvedResp.getStatus());
        verify(eventProducer).publishEvent("SR-1001", EventType.REQUEST_RESOLVED, RequestStatus.RESOLVED);

        // Step 3: RESOLVED -> CLOSED
        ServiceRequestResponseDto closedResp = serviceRequestService.closeRequest(10L);
        assertEquals(RequestStatus.CLOSED, closedResp.getStatus());
        verify(eventProducer).publishEvent("SR-1001", EventType.REQUEST_CLOSED, RequestStatus.CLOSED);
    }

    @Test
    @DisplayName("7. Should throw InvalidStatusTransitionException when attempting illegal transition (OPEN -> RESOLVED)")
    void shouldThrowExceptionOnIllegalTransitionOpenToResolved() {
        sampleRequest.setStatus(RequestStatus.OPEN);
        when(serviceRequestRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(sampleRequest));

        UpdateRequestStatusDto dto = new UpdateRequestStatusDto(RequestStatus.RESOLVED);

        assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.updateStatus(10L, dto));
        verify(serviceRequestRepository, never()).save(any(ServiceRequest.class));
    }

    @Test
    @DisplayName("7b. Should throw InvalidStatusTransitionException when moving to IN_PROGRESS without assignment")
    void shouldThrowExceptionWhenMovingToInProgressWithoutAssignment() {
        sampleRequest.setStatus(RequestStatus.OPEN);
        sampleRequest.setAssignedTo(null);
        when(serviceRequestRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(sampleRequest));

        UpdateRequestStatusDto dto = new UpdateRequestStatusDto(RequestStatus.IN_PROGRESS);

        assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.updateStatus(10L, dto));
    }

    @Test
    @DisplayName("8. Should reject modification attempts on CLOSED requests")
    void shouldRejectModificationOnClosedRequest() {
        sampleRequest.setStatus(RequestStatus.CLOSED);
        when(serviceRequestRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(sampleRequest));

        AssignRequestDto assignDto = new AssignRequestDto("agent_new");
        assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.assignRequest(10L, assignDto));

        UpdateRequestStatusDto statusDto = new UpdateRequestStatusDto(RequestStatus.IN_PROGRESS);
        assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.updateStatus(10L, statusDto));

        assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.closeRequest(10L));
        assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.cancelRequest(10L));
    }

    @Test
    @DisplayName("9. Should allow cancellation of OPEN request and reject cancellation of CLOSED/RESOLVED request")
    void shouldHandleCancellationWorkflow() {
        // Successful cancel from OPEN
        sampleRequest.setStatus(RequestStatus.OPEN);
        when(serviceRequestRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(sampleRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        ServiceRequestResponseDto cancelResp = serviceRequestService.cancelRequest(10L);
        assertEquals(RequestStatus.CANCELLED, cancelResp.getStatus());
        verify(eventProducer).publishEvent("SR-1001", EventType.REQUEST_CANCELLED, RequestStatus.CANCELLED);

        // Rejection from CLOSED
        sampleRequest.setStatus(RequestStatus.CLOSED);
        assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.cancelRequest(10L));

        // Rejection from RESOLVED
        sampleRequest.setStatus(RequestStatus.RESOLVED);
        assertThrows(InvalidStatusTransitionException.class, () -> serviceRequestService.cancelRequest(10L));
    }

    @Test
    @DisplayName("10. Should publish Kafka events accurately for request lifecycle")
    void shouldPublishKafkaEventsAccurately() {
        sampleRequest.setStatus(RequestStatus.ASSIGNED);
        sampleRequest.setAssignedTo("agent_sarah");
        when(serviceRequestRepository.findByIdWithCustomer(10L)).thenReturn(Optional.of(sampleRequest));
        when(serviceRequestRepository.save(any(ServiceRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // Trigger in progress
        serviceRequestService.updateStatus(10L, new UpdateRequestStatusDto(RequestStatus.IN_PROGRESS));
        verify(eventProducer).publishEvent(eq("SR-1001"), eq(EventType.REQUEST_STARTED), eq(RequestStatus.IN_PROGRESS));

        // Trigger resolved
        serviceRequestService.updateStatus(10L, new UpdateRequestStatusDto(RequestStatus.RESOLVED));
        verify(eventProducer).publishEvent(eq("SR-1001"), eq(EventType.REQUEST_RESOLVED), eq(RequestStatus.RESOLVED));
    }

    @Test
    @DisplayName("Should filter service requests by status, priority, and category")
    void shouldFilterServiceRequests() {
        when(serviceRequestRepository.findWithFilters(RequestStatus.OPEN, Priority.HIGH, "Account Access"))
                .thenReturn(List.of(sampleRequest));

        List<ServiceRequestResponseDto> results = serviceRequestService.getRequests(RequestStatus.OPEN, Priority.HIGH, "Account Access");

        assertEquals(1, results.size());
        assertEquals("SR-1001", results.get(0).getRequestNumber());
    }
}
