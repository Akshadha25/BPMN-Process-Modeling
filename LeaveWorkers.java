package com.example.leave.worker;

import com.example.leave.service.LeaveService;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.annotation.Variable;
import io.camunda.client.api.response.ActivatedJob;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Job workers for the two service-task types in leave-request.bpmn. */
@Component
public class LeaveWorkers {

    private final LeaveService service;

    public LeaveWorkers(LeaveService service) {
        this.service = service;
    }

    /** Service task "Validate leave request". Writes: valid, validationMessage. */
    @JobWorker(type = "validate-leave")
    public Map<String, Object> validate(@Variable Long requestId) {
        return service.validate(requestId);
    }

    /**
     * Service tasks "Finalize as approved / rejected" (both use type finalize-leave).
     * Reads the whole variable map because managerComment / valid may be missing on the invalid path.
     */
    @JobWorker(type = "finalize-leave")
    public void finalizeLeave(ActivatedJob job) {
        Map<String, Object> variables = job.getVariablesAsMap();
        Long id = ((Number) variables.get("requestId")).longValue();
        boolean approved = "APPROVED".equals(variables.get("outcome"));
        boolean valid = !Boolean.FALSE.equals(variables.get("valid"));
        service.finalizeRequest(
                id,
                approved,
                valid,
                (String) variables.getOrDefault("managerComment", ""),
                (String) variables.getOrDefault("validationMessage", "OK"));
    }
}
