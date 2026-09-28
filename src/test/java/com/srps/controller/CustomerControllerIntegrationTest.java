package com.srps.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srps.dto.CreateCustomerDto;
import com.srps.dto.CustomerResponseDto;
import com.srps.exception.CustomerNotFoundException;
import com.srps.service.CustomerService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @Test
    @DisplayName("POST /api/customers - Should create customer and return 201 Created")
    void shouldCreateCustomer() throws Exception {
        CreateCustomerDto dto = new CreateCustomerDto("Ravi Kumar", "ravi.kumar@example.com");
        CustomerResponseDto responseDto = new CustomerResponseDto(1L, "Ravi Kumar", "ravi.kumar@example.com", LocalDateTime.now());

        when(customerService.createCustomer(any(CreateCustomerDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Ravi Kumar"))
                .andExpect(jsonPath("$.email").value("ravi.kumar@example.com"));
    }

    @Test
    @DisplayName("POST /api/customers - Should return 400 Bad Request when validation fails")
    void shouldReturnBadRequestWhenValidationFails() throws Exception {
        CreateCustomerDto invalidDto = new CreateCustomerDto("", "invalid-email");

        mockMvc.perform(post("/api/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("GET /api/customers - Should return 200 OK with list of customers")
    void shouldReturnAllCustomers() throws Exception {
        CustomerResponseDto customer = new CustomerResponseDto(1L, "Ravi Kumar", "ravi.kumar@example.com", LocalDateTime.now());
        when(customerService.getAllCustomers()).thenReturn(List.of(customer));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Ravi Kumar"));
    }

    @Test
    @DisplayName("GET /api/customers/{id} - Should return 404 Not Found when customer does not exist")
    void shouldReturnNotFoundForMissingCustomer() throws Exception {
        when(customerService.getCustomerById(999L)).thenThrow(new CustomerNotFoundException("Customer not found with id: 999"));

        mockMvc.perform(get("/api/customers/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Customer not found with id: 999"));
    }
}
