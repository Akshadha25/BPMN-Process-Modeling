package com.example.leave.service;

import com.example.leave.controller.DecisionRequest;
import com.example.leave.controller.SubmitLeaveRequest;
import com.example.leave.model.LeaveBalance;
import com.example.leave.model.LeaveRequest;
import com.example.leave.model.LeaveStatus;
import com.example.leave.repository.LeaveBalanceRepository;
import com.example.leave.repository.LeaveRequestRepository;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRequestRepository requests;
    private final LeaveBalanceRepository balances;
    private final WorkflowGateway workflow;

    public LeaveService(LeaveRequestRepository requests,
                        LeaveBalanceRepository balances,
                        WorkflowGateway workflow) {
        this.requests = requests;
        this.balances = balances;
        this.workflow = workflow;
    }

    /**
     * Step 1-2 of the flow: save the request, then start the Camunda process.
     * Deliberately NOT one transaction: the request must be committed before the process starts,
     * because the validate-leave worker may run immediately.
     */
    public LeaveRequest submit(SubmitLeaveRequest in) {
        LeaveRequest request = new LeaveRequest();
        request.setEmployeeName(in.employeeName());
        request.setEmployeeEmail(in.employeeEmail());
        request.setLeaveType(in.leaveType().toUpperCase());
        request.setStartDate(in.startDate());
        request.setEndDate(in.endDate());
        request.setDays(LeaveRules.workingDays(in.startDate(), in.endDate()));
        request.setReason(in.reason());
        request.setStatus(LeaveStatus.SUBMITTED);
        request.setStatusMessage("Submitted");
        request = requests.save(request);

        balance(in.employeeEmail());   // make sure the employee has a balance record

        Map<String, Object> variables = new HashMap<>();
        variables.put("requestId", request.getId());
        variables.put("employeeEmail", request.getEmployeeEmail());
        variables.put("leaveType", request.getLeaveType());
        variables.put("days", request.getDays());
        variables.put("startDate", request.getStartDate().toString());
        variables.put("endDate", request.getEndDate().toString());

        long processInstanceKey = workflow.startProcess(variables);
        requests.setProcessInstanceKey(request.getId(), processInstanceKey);

        // re-read: a worker may already have updated the status
        return requests.findById(request.getId()).orElse(request);
    }

    /** Manager decision from the React inbox: completes the Camunda user task. */
    public void decide(Long id, DecisionRequest decision) {
        LeaveRequest request = requests.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        if (request.getStatus() != LeaveStatus.PENDING_APPROVAL || request.getProcessInstanceKey() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request is not awaiting approval");
        }

        String comment = decision.comment() == null ? "" : decision.comment();
        boolean completed;
        try {
            completed = workflow.completeManagerTask(request.getProcessInstanceKey(), decision.approved(), comment);
        } catch (RuntimeException e) {
            log.warn("Could not complete manager task for request {}: {}", id, e.getMessage());
            completed = false;
        }
        if (!completed) {
            // the frontend retries 409 for a few seconds (task not indexed yet / already completed)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Manager task is not available yet, retry shortly");
        }
    }

    /** validate-leave worker: checks dates and annual balance, moves the request to PENDING_APPROVAL if valid. */
    @Transactional
    public Map<String, Object> validate(Long requestId) {
        LeaveRequest request = requests.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Leave request " + requestId + " not found"));

        int remaining = balance(request.getEmployeeEmail()).getRemainingDays();
        LeaveRules.Result result = LeaveRules.validate(request.getLeaveType(), request.getStartDate(),
                request.getEndDate(), request.getDays(), remaining);

        if (result.valid()) {
            request.setStatus(LeaveStatus.PENDING_APPROVAL);
            request.setStatusMessage("Awaiting manager approval");
        } else {
            request.setStatusMessage(result.message());
        }
        requests.save(request);

        return Map.of("valid", result.valid(), "validationMessage", result.message());
    }

    /**
     * finalize-leave worker: stores the outcome; deducts ANNUAL leave only after approval.
     * Idempotent: a retried/duplicate job for an already finalised request does nothing,
     * so the balance can never be deducted twice.
     */
    @Transactional
    public void finalizeRequest(Long requestId, boolean approved, boolean valid,
                                String managerComment, String validationMessage) {
        LeaveRequest request = requests.findById(requestId)
                .orElseThrow(() -> new IllegalStateException("Leave request " + requestId + " not found"));

        if (request.getStatus() == LeaveStatus.APPROVED || request.getStatus() == LeaveStatus.REJECTED) {
            log.info("Request {} already finalised as {}, ignoring duplicate job", requestId, request.getStatus());
            return;
        }

        String comment = managerComment == null ? "" : managerComment.trim();
        if (approved) {
            request.setStatus(LeaveStatus.APPROVED);
            request.setStatusMessage(comment.isEmpty() ? "Approved by manager" : "Approved: " + comment);
            if ("ANNUAL".equalsIgnoreCase(request.getLeaveType())) {
                LeaveBalance balance = balance(request.getEmployeeEmail());
                balance.setRemainingDays(Math.max(0, balance.getRemainingDays() - request.getDays()));
                balances.save(balance);
            }
        } else {
            request.setStatus(LeaveStatus.REJECTED);
            if (!valid) {
                request.setStatusMessage(validationMessage);
            } else {
                request.setStatusMessage(comment.isEmpty() ? "Rejected by manager" : "Rejected by manager: " + comment);
            }
        }
        requests.save(request);
        notifyEmployee(request);
    }

    /** Existing balance, or a new one with the default entitlement. */
    @Transactional
    public LeaveBalance balance(String employeeEmail) {
        return balances.findById(employeeEmail)
                .orElseGet(() -> balances.save(new LeaveBalance(employeeEmail, LeaveRules.DEFAULT_ANNUAL_DAYS)));
    }

    /** Notification hook (stand-in for an e-mail). */
    private void notifyEmployee(LeaveRequest request) {
        log.info("[notification] To {}: your {} leave request #{} is {} - {}",
                request.getEmployeeEmail(), request.getLeaveType(), request.getId(),
                request.getStatus(), request.getStatusMessage());
    }
}
