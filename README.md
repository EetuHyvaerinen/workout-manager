# Workout Manager: Full-Stack System Implementation

A high-performance, dependency-free workout management system featuring a custom Java backend and a dynamic vanilla JavaScript frontend

A live demo is available [here](https://www.hyvaerinen.com/workoutmanager/login?email=test@test&pass=testtest)

---

## Technical Specifications

| Component | Technology |
|-----------|------------|
| Runtime | Java 21 |
| Database | MySQL |
| Frontend | JavaScript, HTML, CSS |
| API Protocol | REST / JSON |

---

## System Architecture

The backend is built on a custom HTTP server using Java virtual threads for high-throughput, low-latency request handling

Custom Infrastructure engineered for minimal overhead and zero external dependencies

---

## Core Application Logic

The system provides advanced scheduling and tracking features to deliver a complete workout experience

Log current or past workouts, plan future sessions, and handle missed workouts with ease

Automatic progressive overloading that applies an increased total load to your next workout plan

---

## Custom Systems

- Recursive descent JSON parser and serializer implemented from scratch
- Asynchronous producer–consumer logging system using `LinkedBlockingQueue` with log rotation and backpressure handling
- Thread-safe token bucket rate limiter to manage API request frequency
- Custom thread-safe JDBC connection pool
- Authenticator handling JWT, cookies, and user authentication

---

## Security

Stateless JWTs verified with HMAC SHA-256, stored in secure HttpOnly cookies

User passwords are secured using PBKDF2 with per-user salts and a high iteration count

Role-based access control separating user and admin privileges

---

## Frontend Implementation

The frontend is a lightweight, dependency-free single page application built using vanilla JavaScript, HTML, and CSS

- Create, browse and delete workouts
- Custom calendar showing completed, planned, and missed workouts
- Displays workouts based on the selected day
- Native JavaScript state management for complex workout flows
- Fetch API with credentials and CORS handling for secure backend communication
- Custom modal overlay for confirmations

---

## Admin Dashboard

Provides internal observability through custom logging

- Displays system metrics including uptime, memory usage, and database pool saturation
- Log viewer for retrieving and filtering logs from the internal LogDispatcher
- Aggregated statistics on user activity and workout frequency

---

## Setup & Deployment

1. Initialize the MySQL database using the provided schema
2. Set the following environment variables:

| Variable | Purpose |
|----------|---------|
| DB_URL | MySQL connection string (e.g., jdbc:mysql://localhost:3306/workoutmanager) |
| DB_USER | Database username |
| DB_PASSWORD | Database password |
| SecretKeyWorkoutHelper | JWT signing key (32+ characters) |
| SERVER_ORIGIN | Allowed CORS origin (e.g., https://your-domain.com) |

3. Execute `Server.java` to start the backend server
4. Update frontend API URLs to match your backend server
5. Serve frontend files using any static web server
