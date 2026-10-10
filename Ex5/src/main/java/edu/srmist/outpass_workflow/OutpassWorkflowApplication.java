package edu.srmist.outpass_workflow;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Deploys the BPMN model and the Warden form to the SaaS cluster on start-up,
 * so there is no need to click "Deploy" in Web Modeler.
 */
@SpringBootApplication
@Deployment(resources = {
        "classpath:bpmn/hostel-outpass.bpmn",
        "classpath:bpmn/warden-approval.form"})
public class OutpassWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(OutpassWorkflowApplication.class, args);
    }
}
