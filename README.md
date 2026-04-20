# Instructions for candidates

This is the Java version of the Payment Gateway challenge. If you haven't already read this [README.md](https://github.com/cko-recruitment/) on the details of this exercise, please do so now.

## Requirements
- JDK 17
- Docker

## Template structure

src/ - A skeleton SpringBoot Application

test/ - Some simple JUnit tests

imposters/ - contains the bank simulator configuration. Don't change this

.editorconfig - don't change this. It ensures a consistent set of rules for submissions when reformatting code

docker-compose.yml - configures the bank simulator


## Payment Gateway - Design Considerations & Assumptions

**Overview**

This solution implements a payment gateway that allows merchants to:

- Process card payments
- Retrieve previously processed payments

The gateway validates requests, interacts with a simulated acquiring bank, and returns appropriate responses based on the outcome.

**Key Design Considerations**

**1\. API Design**

- **POST /payment**
  - Processes a payment
  - Returns:
    - 200 OK: Authorized / Declined
    - 400 Bad Request: Validation failure (Rejected)
- **GET /payment/{id}**
  - Retrieves payment details
  - Returns:
    - 200 OK: Payment found
    - 404 Not Found: Invalid ID

**2\. Separation of Concerns**

The system follows a layered architecture:

- **Controller**: Handles HTTP requests/responses
- **Validator:** Validate input payment request
- **Service**: Business logic (bank integration, mapping)
- **Repository**: In-memory storage
- **Model**: DTOs

This improves maintainability and testability.

**3\. Validation Handling (Rejected)**

- All validation is performed at the gateway level
- Invalid requests are **not sent to the acquiring bank**
- Response:
  - **HTTP 400 Bad Request**
  - Clear validation message

**Example:**

{

"message": "card_number must be between 14 and 19 digits"

}

**Design Decision:**

- "Rejected" is treated as **validation failure**
- No payment is created or stored

**4\. Payment Status Handling**

| **Scenario**       | **Status** | **HTTP** |
| ------------------ | ---------- | -------- |
| Validation failure | (Rejected) | 400      |
| Bank authorized    | AUTHORIZED | 200      |
| Bank declined      | DECLINED   | 200      |
| Bank failure (5XX) | PENDING    | 5XX      |

**5\. Bank Integration**

- Integrated via HTTP call to simulator:

<http://localhost:8080/payments>

**Mapping:**

- authorized = true AUTHORIZED
- authorized = false DECLINED
- 5XX response: treated as **PENDING**

**6\. Handling Bank Failures (5XX)**

- When bank returns **5XX (e.g., 503)**:
  - Payment status is set to **PENDING**
  - Payment is **stored**
  - Response includes **payment ID**

**Example:**

{

"id": "uuid",

"status": "PENDING",

"message": "Bank service unavailable. Please retry or check status later."

}

**Rationale:**

- Request reached bank but outcome unknown
- Enables reconciliation using GET API

**7\. Retry Mechanism**

- Designed to handle transient bank failures (5XX)
- Improves reliability and resiliency

**8\. Circuit Breaker**

- Considered for bank integration
- Prevents repeated calls to failing downstream system
- Avoids cascading failures
- Can be implemented using Resilience4j

**10\. Payment Storage**

- Uses **in-memory repository**

**11\. Error Handling**

- Centralized using **Global Exception Handler**

**Responsibilities:**

- Map exceptions to HTTP status codes
- Ensure consistent error responses

**Examples:**

- Validation errors: 400
- Payment not found: 404
- Bank failure: handled as PENDING / 5XX

**Assumptions**

- Only 3 currencies are supported:
  - GBP, USD, EUR
- Amount is provided in **minor currency units**
- CVV is numeric (3-4 digits)
- Validation failures:
  - Do not create payment records
- Bank failures (5XX):
  - Result in **PENDING status**
  - Payment is stored for later retrieval
- Client is expected to:
  - Use GET /payment/{id} to check final status of pending payments
- Retry and circuit breaker:
  - Retry for 5XX error from bank api.


