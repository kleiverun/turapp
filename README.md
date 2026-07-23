# turapp — backend

Spring Boot REST API for the turapp trip-tracking application. Handles users, trips, trackpoints, and planned routes. Pairs with the [Android frontend](https://github.com/kleiverun/turappfront).

## Tech stack

| | |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security (`permitAll` — no auth yet) |
| Database | MySQL (JPA / Hibernate) |
| Build tool | Maven |
| Extra | GPX file import, WebSocket support |

## Project structure

```
src/main/java/com/ole/turapp/
├── controller/
│   ├── UserController.java        # POST /api/users
│   ├── TripController.java        # CRUD under /api/users/{userId}/trips
│   ├── TrackPointController.java  # POST /api/trackpoints, GET /api/trips/{id}/trackpoints
│   └── RouteController.java       # Planned routes + GPX import
├── service/
│   ├── UserService.java
│   ├── TripService.java
│   ├── TrackPointService.java
│   ├── RouteService.java
│   └── GpxRouteParser.java        # Parses GPX <rte> elements into routes
├── model/                         # JPA entities: User, Trip, TrackPoint, Route, RoutePoint
├── repository/                    # Spring Data repositories
├── dto/                           # Request/response DTOs
├── exception/                     # GlobalExceptionHandler, NotFoundException, ApiError
└── config/
    └── SecurityConfig.java        # permitAll (no auth yet)
```

## API endpoints

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/users` | Register a user |

### Trips
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/users/{userId}/trips` | Create a trip |
| `GET`  | `/api/users/{userId}/trips` | List trips for a user |
| `GET`  | `/api/users/{userId}/trips/{tripId}` | Get a single trip |
| `PATCH`| `/api/users/{userId}/trips/{tripId}` | Update a trip |
| `DELETE`| `/api/users/{userId}/trips/{tripId}` | Delete a trip |
| `POST` | `/api/users/{userId}/trips/{tripId}/end` | End a trip (records duration/distance) |

### Trackpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/trackpoints` | Save trackpoints (batch) |
| `GET`  | `/api/trips/{tripId}/trackpoints` | Get trackpoints for a trip |

### Planned routes
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/users/{userId}/routes` | Create a route (JSON) |
| `POST` | `/api/users/{userId}/routes/import` | Import routes from a GPX file |
| `GET`  | `/api/users/{userId}/routes` | List routes for a user |
| `GET`  | `/api/users/{userId}/routes/with-points` | List routes with all their points |
| `GET`  | `/api/routes/{routeId}/points` | Get points for a route |
| `PATCH`| `/api/users/{userId}/routes/{routeId}` | Update a route |
| `DELETE`| `/api/users/{userId}/routes/{routeId}` | Delete a route |

## Running locally

### Prerequisites
- JDK 25
- MySQL running on `localhost:3306`
- A database named `turapp`

### Setup

1. Create the database:
   ```sql
   CREATE DATABASE turapp;
   ```

2. Configure `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/turapp
   spring.datasource.username=root
   spring.datasource.password=your_password
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

   spring.servlet.multipart.max-file-size=25MB
   spring.servlet.multipart.max-request-size=25MB
   ```

3. Build and run:
   ```bash
   ./mvnw spring-boot:run
   ```

   The server starts on port `8080`.

### Android emulator
The emulator reaches the host machine's localhost at `http://10.0.2.2:8080/`.

### Physical Android device
The device must be on the same Wi-Fi as the PC. Use the PC's LAN IP, e.g. `http://192.168.0.207:8080/`. Allow inbound TCP port 8080 in Windows Firewall.

## Known limitations / future work

- No authentication — all endpoints use `permitAll`. JWT-based auth is planned.
- No background job for cleaning up orphaned data.
- GPX import only handles `<rte>` elements (not `<trk>`).
