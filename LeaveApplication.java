package com.example.leave;

import io.camunda.client.annotation.Deployment;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Deploys leave-request.bpmn and leave-approval.form to Camunda when the backend starts. */
@SpringBootApplication
@Deployment(resources = {
        "classpath*:/bpmn/**/*.bpmn",
        "classpath*:/bpmn/**/*.form"
})
public class LeaveApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeaveApplication.class, args);
    }
}
