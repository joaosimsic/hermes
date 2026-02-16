# Events Catalog

## Auth Service Events (Producer)
Produced when identity state changes in Keycloak/Cognito.

| Event Type | Exchange | Routing Key | Description |
| :--- | :--- | :--- | :--- |
| `USER_REGISTERED` | `auth.exchange` | `auth.user.registered` | Triggered when a new user signs up or logs in via GitHub for the first time. |
| `USER_EMAIL_UPDATED` | `auth.exchange` | `auth.user.email.updated` | Triggered when a user changes their primary email. |

## User Service Events (Producer)
Produced when local profile data is modified.

| Event Type | Exchange | Routing Key | Description |
| :--- | :--- | :--- | :--- |
| `USER_CREATED` | `user.exchange` | `user.created` | Local profile creation confirmed. |
| `USER_UPDATED` | `user.exchange` | `user.updated` | Profile details (like name) updated. |
| `USER_DELETED` | `user.exchange` | `user.deleted` | User profile removed from system. |

## Schema Locations
JSON Schemas for these events are located in `/events/schemas/events/`.
