package com.srps.service;

import com.srps.dto.CreateCustomerDto;
import com.srps.dto.CustomerResponseDto;
import com.srps.entity.Customer;
import com.srps.exception.CustomerNotFoundException;
import com.srps.exception.DuplicateRequestException;
import com.srps.repository.CustomerRepository;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer sampleCustomer;

    @BeforeEach
    void setUp() {
        sampleCustomer = new Customer(1L, "Ravi Kumar", "ravi.kumar@example.com", LocalDateTime.now());
    }

    @Test
    @DisplayName("Should create customer successfully when email is unique")
    void shouldCreateCustomerSuccessfully() {
        CreateCustomerDto dto = new CreateCustomerDto("Ravi Kumar", "ravi.kumar@example.com");
        when(customerRepository.existsByEmail(dto.getEmail())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenReturn(sampleCustomer);

        CustomerResponseDto response = customerService.createCustomer(dto);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Ravi Kumar", response.getName());
        assertEquals("ravi.kumar@example.com", response.getEmail());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw DuplicateRequestException when creating customer with existing email")
    void shouldThrowExceptionWhenCustomerEmailExists() {
        CreateCustomerDto dto = new CreateCustomerDto("Ravi Kumar", "ravi.kumar@example.com");
        when(customerRepository.existsByEmail(dto.getEmail())).thenReturn(true);

        assertThrows(DuplicateRequestException.class, () -> customerService.createCustomer(dto));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should find customer by ID successfully")
    void shouldFindCustomerById() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        CustomerResponseDto response = customerService.getCustomerById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Ravi Kumar", response.getName());
    }

    @Test
    @DisplayName("Should throw CustomerNotFoundException when customer ID does not exist")
    void shouldThrowExceptionWhenCustomerNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> customerService.getCustomerById(99L));
    }

    @Test
    @DisplayName("Should return list of all customers")
    void shouldReturnAllCustomers() {
        when(customerRepository.findAll()).thenReturn(List.of(sampleCustomer));

        List<CustomerResponseDto> list = customerService.getAllCustomers();

        assertEquals(1, list.size());
        assertEquals("Ravi Kumar", list.get(0).getName());
    }
}
