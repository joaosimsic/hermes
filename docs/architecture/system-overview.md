# System Overview: Hermes Architecture

Hermes is built on a **Microservices Architecture** using **Event-Driven Design (EDA)** to ensure data consistency and service decoupling.



## Core Components
* **API Gateway**: The entry point for all requests. Handles JWT validation, Rate Limiting, and Circuit Breaking.
* **Auth Service**: Manages identities via Keycloak (Dev) or AWS Cognito (Prod). It acts as a producer of identity events.
* **User Service**: Manages local user profiles. It consumes events from the Auth Service to synchronize data and publishes its own lifecycle events.
* **Message Broker (RabbitMQ)**: Facilitates asynchronous communication between services.

## Event Flow (Example: Registration)
1.  **Client** sends a request to `/api/auth/register`.
2.  **Auth Service** creates the user in the Identity Provider and saves a `USER_REGISTERED` event to its **Outbox** table.
3.  **Outbox Relay** picks up the event and publishes it to `auth.exchange`.
4.  **User Service** consumes the message and creates a local profile record.
