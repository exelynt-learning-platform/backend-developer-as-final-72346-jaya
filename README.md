# Resource Booking API

Secure RESTful Resource Booking System built with Spring Boot 3, Java 17, Spring Security, JWT, JPA/Hibernate, and SQL database support.

## Features

- JWT login with `POST /auth/login`
- `ADMIN` and `USER` role-based access control
- Admin CRUD access for resources and reservations
- User read-only access to resources
- User reservation creation using identity from the JWT, not the request body
- Users can view only their own reservations
- Reservation statuses: `PENDING`, `CONFIRMED`, `CANCELLED`
- Reservation price stored as decimal and calculated from resource hourly price
- Reservation filtering by `status`, `minPrice`, and `maxPrice`
- Pagination with `page` and `size`
- Optional sorting with Spring Data `sort`, for example `sort=price,desc`
- Bean validation and structured JSON error responses
- Swagger/OpenAPI documentation
- Seed users and sample resources for testing

## Tech Stack

- Java 17+
- Spring Boot 3.3
- Spring Web
- Spring Security
- Spring Data JPA / Hibernate
- JWT via `jjwt`
- PostgreSQL or MySQL
- H2 in-memory database for quick local testing
- Springdoc OpenAPI

## Seed Users

| Role | Email | Password |
| --- | --- | --- |
| ADMIN | `admin@example.com` | `admin123` |
| USER | `user@example.com` | `user123` |
| USER | `user2@example.com` | `user2123` |

## Running Locally

The default configuration uses H2 so the app can start immediately:

```bash
mvn spring-boot:run
```

Open Swagger UI:

```text
http://localhost:8080/swagger-ui.html
```

## Environment Variables

| Variable | Description | Default |
| --- | --- | --- |
| `SERVER_PORT` | API port | `8080` |
| `DB_URL` | JDBC URL | H2 in-memory URL |
| `DB_USERNAME` | Database username | `sa` |
| `DB_PASSWORD` | Database password | empty |
| `DB_DRIVER` | JDBC driver class | `org.h2.Driver` |
| `JPA_DDL_AUTO` | Hibernate schema mode | `update` |
| `JWT_SECRET` | JWT signing secret, at least 32 characters | development secret |
| `JWT_EXPIRATION_MS` | Token lifetime in milliseconds | `86400000` |

## PostgreSQL Configuration

```bash
export DB_URL=jdbc:postgresql://localhost:5432/bookingdb
export DB_USERNAME=postgres
export DB_PASSWORD=postgres
export DB_DRIVER=org.postgresql.Driver
export JPA_DDL_AUTO=update
export JWT_SECRET=replace-with-a-secure-32-character-minimum-secret
mvn spring-boot:run
```

## MySQL Configuration

```bash
export DB_URL=jdbc:mysql://localhost:3306/bookingdb
export DB_USERNAME=root
export DB_PASSWORD=password
export DB_DRIVER=com.mysql.cj.jdbc.Driver
export JPA_DDL_AUTO=update
export JWT_SECRET=replace-with-a-secure-32-character-minimum-secret
mvn spring-boot:run
```

## Authentication

Login:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}'
```

Use the returned token:

```bash
curl http://localhost:8080/resources \
  -H "Authorization: Bearer <token>"
```

## Main Endpoints

### Auth

- `POST /auth/login` - authenticate and receive JWT

### Resources

- `GET /resources?page=0&size=10&sort=name,asc` - ADMIN and USER
- `GET /resources/{id}` - ADMIN and USER
- `POST /resources` - ADMIN only
- `PUT /resources/{id}` - ADMIN only
- `DELETE /resources/{id}` - ADMIN only

### Reservations

- `GET /reservations?page=0&size=10&status=PENDING&minPrice=10&maxPrice=100&sort=price,desc`
  - ADMIN sees all reservations
  - USER sees only their own reservations
- `GET /reservations/{id}`
  - ADMIN can view any reservation
  - USER can view only their own reservation
- `POST /reservations`
  - ADMIN and USER can create reservations
  - USER identity is read from the JWT
- `PUT /reservations/{id}` - ADMIN only full reservation update
- `PUT /reservations/{id}/status` - ADMIN only
- `PATCH /reservations/{id}/cancel` - owner or ADMIN
- `DELETE /reservations/{id}` - ADMIN only

Create reservation example:

```bash
curl -X POST http://localhost:8080/reservations \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceId": 1,
    "startTime": "2026-10-01T10:00:00",
    "endTime": "2026-10-01T12:00:00"
  }'
```

Update reservation status as admin:

```bash
curl -X PUT http://localhost:8080/reservations/1/status \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{"status":"CONFIRMED"}'
```

## Error Responses

Errors are returned in a consistent JSON shape:

```json
{
  "timestamp": "2026-08-30T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/reservations",
  "validationErrors": {
    "resourceId": "must not be null"
  }
}
```
