# Week 2 – BPMN Process Modeling

## Overview
This repository contains the BPMN process models created for Week 2. The models were created using **Camunda Modeler** and are based on the three business process scenarios given in the assignment.

The purpose of this activity is to understand and apply the basic BPMN building blocks such as Start Events, Tasks, Exclusive Gateways, Sequence Flows, alternative paths, and End Events.

## Scenarios

### Scenario 1 – Hotel Room Reservation
This process models how a guest books a room through a hotel's online reservation system.

The process starts when the guest submits a room booking request with check-in and check-out dates. The reservation system checks room availability for the requested dates. An exclusive gateway is then used to determine whether rooms are available.

If rooms are unavailable, the system notifies the guest that no rooms are available and the process ends.

If rooms are available, the system requests payment (advance/deposit) from the guest. Another exclusive gateway checks whether the payment was successful. If the payment fails, the system notifies the guest about the payment failure and the process ends.

If the payment is successful, the system confirms the booking and generates a booking reference number. The system then sends a booking confirmation email with the reservation details to the guest, and the process ends.

### Scenario 2 – Loan Application Processing
This process models how a customer applies for a personal loan at a bank.

The process starts when the customer submits a loan application. The bank's system verifies the applicant's documents and credit score. An exclusive gateway checks whether the documents are valid.

If the documents are incomplete or invalid, the system rejects the application and notifies the customer, and the process ends.

If the documents are valid, the system checks the applicant's eligibility based on credit score and income. Another exclusive gateway determines whether the applicant is eligible. If the applicant is not eligible, the loan officer sends a rejection notification to the customer and the process ends.

If the applicant is eligible, the request is forwarded to the loan officer for final approval. A third exclusive gateway checks the loan officer's decision. If the loan officer approves the loan, the system disburses the loan amount and sends an approval notification. If the loan officer rejects the loan, the system sends a rejection notification to the customer. The process ends after the appropriate notification is sent.

### Scenario 3 – Job Applicant Recruitment Process
This process models how a job application received by a company's HR department is handled.

The process begins when a candidate submits a job application online. The HR system screens the application against the minimum eligibility criteria. An exclusive gateway checks whether the candidate meets the eligibility criteria. If not, the system sends a rejection notification and the process ends.

If the candidate meets the eligibility criteria, the HR team schedules a technical interview. The technical panel evaluates the candidate's performance in the interview. Another exclusive gateway checks the interview result. If the candidate fails the technical interview, HR sends a rejection notification and the process ends.

If the candidate passes the technical interview, the candidate is scheduled for an HR/managerial round. A third exclusive gateway checks the outcome of the HR round. If the candidate is rejected in the HR round, a rejection notification is sent and the process ends.

If the candidate is selected, the system generates an offer letter and sends it to the candidate. The process ends after the offer letter is sent.

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
- `scenario1_hotel_reservation.bpmn` – Hotel Room Reservation
- `scenario2_loan_application.bpmn` – Loan Application Processing
- `scenario3_job_recruitment.bpmn` – Job Applicant Recruitment Process
- `BPMN_Explanation_Report.pdf` – Explanation of the three BPMN processes

## Verification
The BPMN models were opened and checked in Camunda Modeler to ensure that the processes, sequence flows, gateways, alternative paths, and end events were present and readable.

The models were also tested using Token Simulation to check the different alternative paths in the processes.
