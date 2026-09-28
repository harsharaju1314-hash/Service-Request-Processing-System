package com.srps.repository;

import com.srps.entity.ServiceRequest;
import com.srps.enums.Priority;
import com.srps.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    Optional<ServiceRequest> findByRequestNumber(String requestNumber);

    boolean existsByRequestNumber(String requestNumber);

    @Query("SELECT sr FROM ServiceRequest sr JOIN FETCH sr.customer WHERE " +
           "(:status IS NULL OR sr.status = :status) AND " +
           "(:priority IS NULL OR sr.priority = :priority) AND " +
           "(:category IS NULL OR LOWER(sr.category) = LOWER(:category)) " +
           "ORDER BY sr.createdAt DESC")
    List<ServiceRequest> findWithFilters(
            @Param("status") RequestStatus status,
            @Param("priority") Priority priority,
            @Param("category") String category
    );

    @Query("SELECT sr FROM ServiceRequest sr JOIN FETCH sr.customer WHERE sr.id = :id")
    Optional<ServiceRequest> findByIdWithCustomer(@Param("id") Long id);
}
