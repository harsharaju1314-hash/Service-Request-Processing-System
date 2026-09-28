package com.srps.service;

import com.srps.dto.CreateCustomerDto;
import com.srps.dto.CustomerResponseDto;
import com.srps.entity.Customer;
import com.srps.exception.CustomerNotFoundException;
import com.srps.exception.DuplicateRequestException;
import com.srps.repository.CustomerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponseDto createCustomer(CreateCustomerDto dto) {
        log.info("Creating new customer with email: {}", dto.getEmail());

        if (customerRepository.existsByEmail(dto.getEmail())) {
            log.warn("Customer creation failed: email '{}' already exists", dto.getEmail());
            throw new DuplicateRequestException(String.format("Customer with email '%s' already exists", dto.getEmail()));
        }

        Customer customer = new Customer(dto.getName(), dto.getEmail());
        Customer saved = customerRepository.save(customer);
        log.info("Customer created successfully with id: {}", saved.getId());

        return mapToResponseDto(saved);
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDto> getAllCustomers() {
        log.debug("Fetching all customers");
        return customerRepository.findAll().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerResponseDto getCustomerById(Long id) {
        log.debug("Fetching customer by id: {}", id);
        Customer customer = getCustomerEntityById(id);
        return mapToResponseDto(customer);
    }

    @Transactional(readOnly = true)
    public Customer getCustomerEntityById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Customer lookup failed: id '{}' not found", id);
                    return new CustomerNotFoundException(String.format("Customer not found with id: %d", id));
                });
    }

    private CustomerResponseDto mapToResponseDto(Customer customer) {
        return new CustomerResponseDto(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getCreatedAt()
        );
    }
}
