package com.example.leave.service;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * All direct calls to Camunda live here (start a process, complete the manager task),
 * so the rest of the backend does not depend on the Camunda client API.
 */
@Component
public class WorkflowGateway {

    public static final String PROCESS_ID = "leave-request";

    private final CamundaClient client;

    public WorkflowGateway(CamundaClient client) {
        this.client = client;
    }

    /** Starts one leave-request process instance and returns its key. */
    public long startProcess(Map<String, Object> variables) {
        ProcessInstanceEvent instance = client.newCreateInstanceCommand()
                .bpmnProcessId(PROCESS_ID)
                .latestVersion()
                .variables(variables)
                .send()
                .join();
        return instance.getProcessInstanceKey();
    }

    /**
     * Finds the Manager approval user task of the instance and completes it.
     * Returns false if the task is not visible yet (it is indexed a moment after it is created).
     */
    public boolean completeManagerTask(long processInstanceKey, boolean approved, String comment) {
        var tasks = client.newUserTaskSearchRequest()
                .filter(f -> f.processInstanceKey(processInstanceKey))
                .send()
                .join()
                .items();
        if (tasks.isEmpty()) {
            return false;
        }
        client.newCompleteUserTaskCommand(tasks.get(0).getUserTaskKey())
                .variables(Map.of("approved", approved, "managerComment", comment))
                .send()
                .join();
        return true;
    }
}
