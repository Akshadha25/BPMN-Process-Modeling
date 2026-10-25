package com.example.leave.repository;

import com.example.leave.model.LeaveRequest;
import com.example.leave.model.LeaveStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByEmployeeEmailOrderByCreatedAtDesc(String employeeEmail);

    List<LeaveRequest> findByStatusOrderByCreatedAtAsc(LeaveStatus status);

    /**
     * Updates only the process instance key. A worker may already have changed the status
     * by the time the key is stored, so saving the whole entity would overwrite that status.
     */
    @Modifying
    @Transactional
    @Query("update LeaveRequest r set r.processInstanceKey = :key where r.id = :id")
    int setProcessInstanceKey(@Param("id") Long id, @Param("key") Long key);
}
