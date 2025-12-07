# 3TIWFullStack2425
Architecture Overview

This project implements a **Spring Cloud microservice system** with **Eureka discovery**, **API Gateway routing**, **Config Server**, **OpenFeign inter-service communication**, and **RabbitMQ messaging**.

# Microservices & Ports

| Service                     | Port                                    | Description                                           |
| --------------------------- | --------------------------------------- | ----------------------------------------------------- |
| **Config Server**           | `8088`                                  | Centralized configuration provider.                   |
| **Eureka Discovery Server** | `8061`                                  | Registers all microservices for service discovery.    |
| **API Gateway**             | `8083`                                  | Single entry point, routes requests to microservices. |
| **post-service**            | `8081`                                  | Manages posts, visibility rules, submission workflow. |
| **review-service**          | `8082`                                  | Manages reviews, approval/rejection flow.             |
| **comment-service**         | `8084`                                  | Handles comments for published posts.                 |
| **RabbitMQ**                | `5672` (or Testcontainers dynamic port) | Messaging platform.                                   |
| **MySQL**                   | `3306` (or Testcontainers dynamic port) | Per-service database.                                 |

Each microservice has its **own database** and is fully isolated.


# API Gateway (8083)

All external requests pass through the API Gateway.

### Routing Rules

| Incoming Path | Target Service         |
| ------------- | ---------------------- |
| `/post/**`    | `lb://post-service`    |
| `/review/**`  | `lb://review-service`  |
| `/comment/**` | `lb://comment-service` |

Gateway also rewrites paths so that:

```
http://localhost:8083/post/api/posts
```

becomes:

```
http://post-service:8081/api/posts
```

Used technologies:

* **Spring Cloud Gateway**
* **Spring Cloud Config**
* **Open Feigh**
* **Eureka Discovery**
* **Rabbit MQ**
* **JUnit**

---

# Eureka Discovery Server (8061)

Every microservice registers itself with Eureka at startup:

* post-service
* review-service
* comment-service
* api-gateway
* config-service

Because of Eureka, services communicate using **service names**, not IP addresses.

Example:

```java
@FeignClient(name = "post-service")
```

Eureka resolves the real location automatically.

---

# Spring Cloud Config Server (8088)

Loads configuration for all services through:

```
spring.config.import=configserver:http://localhost:8071/
```

This allows:

* centralized configuration
* environment profiles
* no duplicated config files

---

# OpenFeign Inter-Service Communication

OpenFeign is used for internal HTTP communication:

### review-service → post-service

Used to validate that a post exists:

```java
postServiceClient.getPostById(postId, "internal");
```

### comment-service → post-service

Used to check post visibility before returning comments:

```java
postServiceClient.getPostById(postId, "internal");
```


# Messaging (RabbitMQ)

Used for **loosely coupled communication** between review-service and post-service.

### Messaging Resources

| Component   | Name                |
| ----------- | ------------------- |
| Exchange    | `review.exchange`   |
| Routing Key | `post.reviewed`     |
| Queue       | `review.decisions`  |
| Event       | `PostReviewedEvent` |


# Event Flow

### Author submits post for review

post-service → review-service via **Feign**

Post becomes:

```
PENDING_REVIEW
```

---

### Reviewer approves or rejects

review-service publishes a message:

```
Exchange: review.exchange
Routing Key: post.reviewed
Queue: review.decisions
Payload: PostReviewedEvent(postId, "APPROVED" or "REJECTED")
```

---

### post-service consumes message

Listener:

```java
@RabbitListener(queues = "review.decisions")
```

Updates the post:

| Decision | New Status |
| -------- | ---------- |
| APPROVED | PUBLISHED  |
| REJECTED | REJECTED   |

---

# Application Flow Summary

### **1. Author creates post → status = DRAFT**

### **2. Author submits post**

* post-service sets `PENDING_REVIEW`
* calls review-service (Feign)

### **3. Reviewer approves/rejects**

* review-service changes review state
* publishes `PostReviewedEvent`

### **4. post-service receives event**

* updates post to `PUBLISHED` or `REJECTED`

### **5. Users add comments**

comment-service checks visibility via Feign:

```java
postServiceClient.getPostById(postId, "internal");
```

If post is not visible → throws exception.

---

# Simple Header-Based Security

All incoming requests include:

```
user: <username>
```

Rules:

* Only the **author** can edit or submit a post.
* Only the **author** or `"internal"` can view non-published posts.
* Reviewer identity is `"editor_mock_id"`.
* Only comment **authors** or `"internal"` can edit/delete comments.

No OAuth or JWT required—fits the assignment constraints.

---

# Testing Strategy

### Unit Testing

* Mockito for mocking repos, Feign clients, and RabbitMQ publisher.
* All service logic covered.

### Controller Testing

* MockMvc for endpoint-level validation.
* GlobalExceptionHandler included for error mapping.

### Integration Testing

Using **Testcontainers**:

* MySQL containers per service
* RabbitMQ container for messaging tests
* Full SpringBoot context

Covers:

* repository + DB
* messaging consumption
* API behavior via MockMvc

---

# Technologies Used

| Concern           | Technology                                |
| ----------------- | ----------------------------------------- |
| Microservices     | Spring Boot 3                             |
| Service Discovery | Eureka                                    |
| Config Server     | Spring Cloud Config                       |
| API Gateway       | Spring Cloud Gateway                      |
| Messaging         | RabbitMQ + Spring AMQP                    |
| REST Clients      | OpenFeign                                 |
| Databases         | MySQL + JPA Hibernate                     |
| Testing           | JUnit 5, Mockito, MockMvc, Testcontainers |
| Logging           | Logback + application.properties          |
| Build             | Maven                                     |


