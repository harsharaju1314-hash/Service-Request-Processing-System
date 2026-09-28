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
import com.srps.exception.DuplicateRequestException;
import com.srps.exception.InvalidStatusTransitionException;
import com.srps.exception.ServiceRequestNotFoundException;
import com.srps.kafka.ServiceRequestEventProducer;
import com.srps.repository.ServiceRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ServiceRequestService {

    private static final Logger log = LoggerFactory.getLogger(ServiceRequestService.class);

    private final ServiceRequestRepository serviceRequestRepository;
    private final CustomerService customerService;
    private final ServiceRequestEventProducer eventProducer;

    public ServiceRequestService(ServiceRequestRepository serviceRequestRepository,
                                 CustomerService customerService,
                                 ServiceRequestEventProducer eventProducer) {
        this.serviceRequestRepository = serviceRequestRepository;
        this.customerService = customerService;
        this.eventProducer = eventProducer;
    }

    @Transactional
    public ServiceRequestResponseDto createRequest(CreateServiceRequestDto dto) {
        log.info("Creating service request with number: '{}' for customerId: {}",
                dto.getRequestNumber(), dto.getCustomerId());

        if (serviceRequestRepository.existsByRequestNumber(dto.getRequestNumber())) {
            log.warn("Duplicate request creation attempt: Request number '{}' already exists", dto.getRequestNumber());
            throw new DuplicateRequestException(
                    String.format("Service request with number '%s' already exists", dto.getRequestNumber())
            );
        }

        Customer customer = customerService.getCustomerEntityById(dto.getCustomerId());

        ServiceRequest request = new ServiceRequest(
                dto.getRequestNumber(),
                customer,
                dto.getCategory(),
                dto.getDescription(),
                dto.getPriority(),
                RequestStatus.OPEN
        );

        ServiceRequest saved = serviceRequestRepository.save(request);
        log.info("Service request '{}' created successfully with id: {}", saved.getRequestNumber(), saved.getId());

        eventProducer.publishEvent(saved.getRequestNumber(), EventType.REQUEST_CREATED, RequestStatus.OPEN);

        return mapToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public ServiceRequestResponseDto getRequestById(Long id) {
        log.debug("Fetching service request by id: {}", id);
        ServiceRequest request = serviceRequestRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> {
                    log.warn("Service request not found with id: {}", id);
                    return new ServiceRequestNotFoundException(String.format("Service request not found with id: %d", id));
                });
        return mapToResponseDto(request);
    }

    @Transactional(readOnly = true)
    public List<ServiceRequestResponseDto> getRequests(RequestStatus status, Priority priority, String category) {
        log.debug("Fetching service requests with filters - status: {}, priority: {}, category: {}",
                status, priority, category);

        List<ServiceRequest> results = serviceRequestRepository.findWithFilters(status, priority, category);
        return results.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ServiceRequestResponseDto assignRequest(Long id, AssignRequestDto dto) {
        log.info("Assigning service request id: {} to: '{}'", id, dto.getAssignedTo());

        ServiceRequest request = findRequestByIdOrThrow(id);

        validateModifiable(request);

        request.setAssignedTo(dto.getAssignedTo());
        if (request.getStatus() == RequestStatus.OPEN) {
            request.setStatus(RequestStatus.ASSIGNED);
        }

        ServiceRequest updated = serviceRequestRepository.save(request);
        log.info("Service request '{}' assigned to '{}' with status '{}'",
                updated.getRequestNumber(), updated.getAssignedTo(), updated.getStatus());

        eventProducer.publishEvent(updated.getRequestNumber(), EventType.REQUEST_ASSIGNED, updated.getStatus());

        return mapToResponseDto(updated);
    }

    @Transactional
    public ServiceRequestResponseDto updateStatus(Long id, UpdateRequestStatusDto dto) {
        RequestStatus newStatus = dto.getStatus();
        log.info("Updating status for request id: {} to: {}", id, newStatus);

        ServiceRequest request = findRequestByIdOrThrow(id);
        RequestStatus currentStatus = request.getStatus();

        validateModifiable(request);

        if (currentStatus == newStatus) {
            log.debug("Request '{}' already in status {}", request.getRequestNumber(), currentStatus);
            return mapToResponseDto(request);
        }

        validateStatusTransition(request, currentStatus, newStatus);

        request.setStatus(newStatus);
        ServiceRequest updated = serviceRequestRepository.save(request);
        log.info("Service request '{}' status updated from {} to {}",
                updated.getRequestNumber(), currentStatus, newStatus);

        EventType eventType = mapStatusToEventType(newStatus);
        eventProducer.publishEvent(updated.getRequestNumber(), eventType, newStatus);

        return mapToResponseDto(updated);
    }

    @Transactional
    public ServiceRequestResponseDto closeRequest(Long id) {
        log.info("Closing service request id: {}", id);

        ServiceRequest request = findRequestByIdOrThrow(id);

        if (request.getStatus() == RequestStatus.CLOSED) {
            log.warn("Request '{}' is already CLOSED", request.getRequestNumber());
            throw new InvalidStatusTransitionException(
                    String.format("Request %s is already CLOSED", request.getRequestNumber())
            );
        }

        if (request.getStatus() != RequestStatus.RESOLVED) {
            log.warn("Cannot close request '{}' from status {}", request.getRequestNumber(), request.getStatus());
            throw new InvalidStatusTransitionException(
                    String.format("Only a RESOLVED request can become CLOSED. Current status is %s", request.getStatus())
            );
        }

        request.setStatus(RequestStatus.CLOSED);
        ServiceRequest updated = serviceRequestRepository.save(request);
        log.info("Service request '{}' successfully closed", updated.getRequestNumber());

        eventProducer.publishEvent(updated.getRequestNumber(), EventType.REQUEST_CLOSED, RequestStatus.CLOSED);

        return mapToResponseDto(updated);
    }

    @Transactional
    public ServiceRequestResponseDto cancelRequest(Long id) {
        log.info("Cancelling service request id: {}", id);

        ServiceRequest request = findRequestByIdOrThrow(id);

        if (request.getStatus() == RequestStatus.CLOSED) {
            log.warn("Cannot cancel CLOSED request '{}'", request.getRequestNumber());
            throw new InvalidStatusTransitionException(
                    String.format("A CLOSED request cannot be cancelled or modified (Request: %s)", request.getRequestNumber())
            );
        }

        if (request.getStatus() == RequestStatus.CANCELLED) {
            log.warn("Request '{}' is already CANCELLED", request.getRequestNumber());
            throw new InvalidStatusTransitionException(
                    String.format("Request %s is already CANCELLED", request.getRequestNumber())
            );
        }

        if (request.getStatus() == RequestStatus.RESOLVED) {
            log.warn("Cannot cancel RESOLVED request '{}'", request.getRequestNumber());
            throw new InvalidStatusTransitionException(
                    String.format("A RESOLVED request cannot be cancelled (Request: %s)", request.getRequestNumber())
            );
        }

        request.setStatus(RequestStatus.CANCELLED);
        ServiceRequest updated = serviceRequestRepository.save(request);
        log.info("Service request '{}' successfully cancelled", updated.getRequestNumber());

        eventProducer.publishEvent(updated.getRequestNumber(), EventType.REQUEST_CANCELLED, RequestStatus.CANCELLED);

        return mapToResponseDto(updated);
    }

    private void validateModifiable(ServiceRequest request) {
        if (request.getStatus() == RequestStatus.CLOSED) {
            log.warn("Modification attempted on CLOSED request '{}'", request.getRequestNumber());
            throw new InvalidStatusTransitionException(
                    String.format("Request %s is CLOSED and cannot be modified", request.getRequestNumber())
            );
        }
        if (request.getStatus() == RequestStatus.CANCELLED) {
            log.warn("Modification attempted on CANCELLED request '{}'", request.getRequestNumber());
            throw new InvalidStatusTransitionException(
                    String.format("Request %s is CANCELLED and cannot be processed further", request.getRequestNumber())
            );
        }
    }

    private void validateStatusTransition(ServiceRequest request, RequestStatus from, RequestStatus to) {
        switch (to) {
            case IN_PROGRESS:
                // Rule 4: A request must be assigned before moving to IN_PROGRESS
                if (from != RequestStatus.ASSIGNED || request.getAssignedTo() == null || request.getAssignedTo().trim().isEmpty()) {
                    throw new InvalidStatusTransitionException(
                            String.format("Request %s cannot move from %s to IN_PROGRESS. It must be ASSIGNED to an agent first.",
                                    request.getRequestNumber(), from)
                    );
                }
                break;

            case RESOLVED:
                // Rule 5: A request must be IN_PROGRESS before it can become RESOLVED
                // Rule 3: Cannot move directly OPEN -> RESOLVED
                if (from != RequestStatus.IN_PROGRESS) {
                    throw new InvalidStatusTransitionException(
                            String.format("Request %s cannot move from %s to RESOLVED. It must be IN_PROGRESS first.",
                                    request.getRequestNumber(), from)
                    );
                }
                break;

            case CLOSED:
                // Rule 6: Only a RESOLVED request can become CLOSED
                if (from != RequestStatus.RESOLVED) {
                    throw new InvalidStatusTransitionException(
                            String.format("Request %s cannot move from %s to CLOSED. Only a RESOLVED request can become CLOSED.",
                                    request.getRequestNumber(), from)
                    );
                }
                break;

            case CANCELLED:
                // Can only cancel OPEN, ASSIGNED, or IN_PROGRESS
                if (from == RequestStatus.RESOLVED) {
                    throw new InvalidStatusTransitionException(
                            String.format("Request %s in status %s cannot be cancelled.", request.getRequestNumber(), from)
                    );
                }
                break;

            case ASSIGNED:
                // Cannot move back to ASSIGNED from RESOLVED
                if (from == RequestStatus.RESOLVED) {
                    throw new InvalidStatusTransitionException(
                            String.format("Request %s cannot move from %s back to ASSIGNED.",
                                    request.getRequestNumber(), from)
                    );
                }
                break;

            case OPEN:
                // Cannot move backwards to OPEN once assigned or in progress
                throw new InvalidStatusTransitionException(
                        String.format("Request %s cannot be reverted from %s back to OPEN.",
                                request.getRequestNumber(), from)
                );

            default:
                throw new InvalidStatusTransitionException(
                        String.format("Unsupported status transition from %s to %s for request %s",
                                from, to, request.getRequestNumber())
                );
        }
    }

    private EventType mapStatusToEventType(RequestStatus status) {
        switch (status) {
            case OPEN:
                return EventType.REQUEST_CREATED;
            case ASSIGNED:
                return EventType.REQUEST_ASSIGNED;
            case IN_PROGRESS:
                return EventType.REQUEST_STARTED;
            case RESOLVED:
                return EventType.REQUEST_RESOLVED;
            case CLOSED:
                return EventType.REQUEST_CLOSED;
            case CANCELLED:
                return EventType.REQUEST_CANCELLED;
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
        }
    }

    private ServiceRequest findRequestByIdOrThrow(Long id) {
        return serviceRequestRepository.findByIdWithCustomer(id)
                .orElseThrow(() -> {
                    log.warn("Service request lookup failed: id '{}' not found", id);
                    return new ServiceRequestNotFoundException(String.format("Service request not found with id: %d", id));
                });
    }

    private ServiceRequestResponseDto mapToResponseDto(ServiceRequest request) {
        return new ServiceRequestResponseDto(
                request.getId(),
                request.getRequestNumber(),
                request.getCustomer() != null ? request.getCustomer().getId() : null,
                request.getCustomer() != null ? request.getCustomer().getName() : null,
                request.getCustomer() != null ? request.getCustomer().getEmail() : null,
                request.getCategory(),
                request.getDescription(),
                request.getPriority(),
                request.getStatus(),
                request.getAssignedTo(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
