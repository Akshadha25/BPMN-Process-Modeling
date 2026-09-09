# Employee Leave Request Approval System

This project models an Employee Leave Request Approval System using BPMN 2.0 and DMN with Camunda 8.

The process automates leave request evaluation based on leave type, requested number of days, and the employee's available leave balance.

## Process Overview

The process follows these steps:

1. The employee submits a leave request with their employee ID, leave type, and requested number of days.
2. The system retrieves the employee's available leave balance.
3. A DMN decision table evaluates the leave request and determines the appropriate route.
4. If the requested days exceed the available balance, the request is immediately rejected with an "Insufficient Balance" notification.
5. Sick leave requests of up to 3 days with sufficient balance are automatically approved.
6. Casual leave, Earned leave, and Sick leave exceeding 3 days are sent to the manager for approval.
7. If the manager rejects the request, the employee is notified and the process ends.
8. If the manager approves the request, the leave information is forwarded to HR, the leave balance is updated, and the employee is notified.

## BPMN Model

The BPMN process uses Camunda 8-compatible elements, including:

- Start Event
- User Tasks
- Service Tasks
- Business Rule Task
- Exclusive Gateways
- Conditional Sequence Flows
- End Events

### Main Variables

The process uses the following variables:

- `employeeId`
- `leaveType`
- `requestedDays`
- `availableBalance`
- `decision`
- `managerDecision`

## DMN Decision Table

The DMN decision table contains the leave approval and routing logic.

It evaluates:

- Available leave balance
- Leave type
- Number of requested days

The decision table uses the **FIRST hit policy** so that insufficient balance is checked before automatic approval.

The possible decisions are:

- `INSUFFICIENT_BALANCE`
- `AUTO_APPROVED`
- `MANAGER_REVIEW`

## Task Types

### User Tasks

User interaction is required for:

- Submit Leave Request
- Manager Approval

### Service Tasks

System actions are handled through Service Tasks, including:

- Fetch Leave Balance
- Forward Leave Information to HR
- Update Leave Balance
- Notify Employee

### Business Rule Task

The Business Rule Task invokes the DMN decision table and stores its result in the `decision` variable.

## Files

- `leave-approval-process.bpmn` — BPMN process model
- `leave-approval-decision.dmn` — DMN decision table

## Tools Used

- Camunda Modeler
- BPMN 2.0
- DMN
- Camunda 8 / Zeebe

## Validation

The BPMN and DMN models are designed to work together, with the Business Rule Task invoking the leave approval decision and the gateway conditions routing the process according to the DMN result.
