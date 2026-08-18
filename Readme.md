# Week 1 – BPMN Process Modeling

## Overview

This repository contains the BPMN process models created for Week 1. The models were created using Camunda Modeler and are based on the three business process scenarios given in the assignment.

The purpose of this activity is to understand and apply the basic BPMN building blocks such as Start Events, Tasks, Exclusive Gateways, Sequence Flows, alternative paths, and End Events.

## Scenarios

### Scenario 1 – Employee Leave Approval

This process models how an employee applies for leave through the company's HR system.

The process starts when the employee submits a leave request. The HR system checks the employee's leave balance. An exclusive gateway is then used to determine whether sufficient leave balance is available.

If there is sufficient leave balance, the request is sent to the manager for approval. The manager can either approve or reject the request. If approved, the employee's leave balance is updated and an approval notification is sent. If rejected, a rejection notification is sent.

If there is insufficient leave balance, the system sends an insufficient-balance notification. The process ends after the appropriate notification is sent.

### Scenario 2 – Online Purchase Order Processing

This process models the steps involved when a customer places an online order.

The process starts when the customer places an order. The system checks whether the product is available. If the product is unavailable, the customer receives an out-of-stock notification and the process ends.

If the product is available, the system processes the payment. An exclusive gateway is used to check whether the payment was successful. If the payment fails, the customer receives a payment failure notification and the process ends.

If the payment is successful, the system confirms the order and prepares the product for shipment. The order is then shipped and the customer receives a shipping confirmation before the process ends.

### Scenario 3 – IT Service Request

This process models how an employee's IT support request is handled.

The process starts when the employee submits an IT support request. The IT help desk registers the request and checks the severity of the problem.

An exclusive gateway is used to decide which technician should handle the problem. A low-severity problem is assigned to a support technician, while a high-severity problem is assigned to a senior technician.

The technician investigates the problem. Another exclusive gateway determines whether the problem can be resolved internally. If it can be resolved, the technician fixes the problem. If it cannot be resolved internally, the problem is escalated to an external service provider.

After the issue is resolved, the help desk updates the request status and the employee receives a resolution notification. The process then ends.

## BPMN Elements Used

The models use the basic BPMN elements required for the assignment:

- Start Event
- Tasks
- Exclusive Gateways
- Sequence Flows
- Alternative Process Paths
- End Events

## Tool Used

The BPMN models were created using **Camunda Modeler**.

## Files

- `scenario1_leave_approval.bpmn` – Employee Leave Approval
- `scenario2_purchase_order.bpmn` – Online Purchase Order Processing
- `scenario3_it_service_request.bpmn` – IT Service Request
- `BPMN_Explanation_Report.pdf` – Explanation of the three BPMN processes

## Verification

The BPMN models were opened and checked in Camunda Modeler to ensure that the processes, sequence flows, gateways, alternative paths, and end events were present and readable.

The models were also tested using Token Simulation to check the different alternative paths in the processes.
