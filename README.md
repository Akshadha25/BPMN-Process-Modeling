# BPMN Process Modeling Assignment

## Overview

This repository contains BPMN process models for three business process scenarios created using Camunda Modeler.

The assignment demonstrates the use of basic BPMN building blocks such as:

- Start Events
- Tasks
- Exclusive Gateways
- Sequence Flows
- Alternative Process Paths
- End Events

## Scenarios

### 1. Employee Leave Approval

This process models how an employee submits a leave request through the company's HR system.

The process includes:
- Leave request submission
- Leave balance checking
- Manager approval
- Approval and rejection paths
- Insufficient leave balance path
- Appropriate notifications
- Process termination

### 2. Online Purchase Order Processing

This process models the processing of an online customer order.

The process includes:
- Customer order placement
- Product availability checking
- Out-of-stock handling
- Payment processing
- Successful and failed payment paths
- Order confirmation
- Product preparation
- Shipment
- Shipping confirmation

### 3. IT Service Request

This process models the handling of an employee IT support request.

The process includes:
- IT support request submission
- Help desk registration
- Problem severity checking
- Low and high severity paths
- Technician investigation
- Internal resolution and external escalation paths
- Request status update
- Resolution notification

## Files

- `scenario1_leave_approval.bpmn` – Employee Leave Approval BPMN model
- `scenario2_purchase_order.bpmn` – Online Purchase Order Processing BPMN model
- `scenario3_it_service_request.bpmn` – IT Service Request BPMN model
- `BPMN_Explanation_Report.pdf` – Explanation of the three scenarios and their corresponding BPMN processes

## Tool Used

The BPMN models were created and tested using **Camunda Modeler**.

## Verification

The BPMN models were checked to ensure that they contain the required BPMN elements, including start events, tasks, exclusive gateways, alternative paths, sequence flows, and end events.

Token Simulation can be used in Camunda Modeler to demonstrate the different alternative paths in each process.

## Repository Structure

```text
BPMN-Assignment/
│
├── README.md
├── BPMN_Explanation_Report.pdf
├── scenario1_leave_approval.bpmn
├── scenario2_purchase_order.bpmn
└── scenario3_it_service_request.bpmn