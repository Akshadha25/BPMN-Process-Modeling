package edu.srmist.outpass_workflow.api;

import io.camunda.client.CamundaClient;
import io.camunda.client.api.response.ProcessInstanceEvent;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/outpass")
public class OutpassController {

    private final CamundaClient camundaClient;

    public OutpassController(CamundaClient camundaClient) {
        this.camundaClient = camundaClient;
    }

    @PostMapping
    public Map<String, Object> applyOutpass(@RequestBody OutpassRequest request) {
        ProcessInstanceEvent instance = camundaClient.newCreateInstanceCommand()
                .bpmnProcessId("hostel-outpass")
                .latestVersion()
                .variables(request)
                .send()
                .join();
        return Map.of("processInstanceKey", instance.getProcessInstanceKey());
    }
}

record OutpassRequest(String studentName, String regNo, String hostelBlock,
                      String outDate, String returnDate, String parentPhone, String purpose) {
}
