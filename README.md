# turapp — backend

Spring Boot REST API for the turapp trip-tracking application. Handles users, trips, trackpoints, planned routes, and map overlays (national parks and nature reserves). Pairs with the React web frontend (`turapp-web/`) and the [Android frontend](https://github.com/kleiverun/turappfront).

## Tech stack

| | |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| Security | Spring Security + JWT |
| Database | MySQL (JPA / Hibernate) |
| Build tool | Maven |
| Extra | GPX file import, national parks & nature reserves from Miljødirektoratet |

## Project structure

```
src/main/java/com/ole/turapp/
├── controller/
│   ├── UserController.java              # POST /api/users, POST /api/users/login
│   ├── TripController.java              # CRUD under /api/users/{userId}/trips
│   ├── TrackPointController.java        # POST /api/trackpoints, GET /api/trips/{id}/trackpoints
│   ├── RouteController.java             # Planned routes + GPX import
│   ├── NationalParkController.java      # GET /api/national-parks
│   └── NatureReserveController.java     # GET /api/nature-reserves
├── service/
│   ├── UserService.java
│   ├── TripService.java
│   ├── TrackPointService.java
│   ├── RouteService.java
│   ├── GpxRouteParser.java              # Parses GPX <rte> elements into routes
│   ├── NationalParkService.java         # Imports/serves national park polygons
│   └── NatureReserveService.java        # Imports/serves nature reserve polygons (paginated)
├── model/                               # JPA entities
├── repository/                          # Spring Data repositories
├── dto/                                 # Request/response DTOs
├── exception/                           # GlobalExceptionHandler, NotFoundException, ApiError
└── config/
    ├── SecurityConfig.java              # JWT filter chain; public: login, register, map overlays
    └── JwtAuthFilter.java
```

## API endpoints

### Users
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/users` | Register a user |
| `POST` | `/api/users/login` | Log in — returns JWT token |

### Trips
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/users/{userId}/trips` | Create a trip |
| `GET`  | `/api/users/{userId}/trips` | List trips for a user |
| `GET`  | `/api/users/{userId}/trips/{tripId}` | Get a single trip |
| `PATCH`| `/api/users/{userId}/trips/{tripId}` | Update a trip |
| `DELETE`| `/api/users/{userId}/trips/{tripId}` | Delete a trip |
| `POST` | `/api/users/{userId}/trips/{tripId}/end` | End a trip |

### Trackpoints
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/trackpoints` | Save trackpoints (batch) |
| `GET`  | `/api/trips/{tripId}/trackpoints` | Get trackpoints for a trip |

### Planned routes
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/users/{userId}/routes` | Create a route |
| `POST` | `/api/users/{userId}/routes/import` | Import from GPX |
| `GET`  | `/api/users/{userId}/routes` | List routes |
| `GET`  | `/api/users/{userId}/routes/with-points` | List routes with points |
| `GET`  | `/api/routes/{routeId}/points` | Get points for a route |
| `PATCH`| `/api/users/{userId}/routes/{routeId}` | Update a route |
| `DELETE`| `/api/users/{userId}/routes/{routeId}` | Delete a route |

### Map overlays (public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/national-parks` | All national park polygons |
| `GET`  | `/api/national-parks/import` | Re-fetch from Miljødirektoratet |
| `GET`  | `/api/nature-reserves` | All nature reserve polygons |
| `GET`  | `/api/nature-reserves/import` | Re-fetch from Miljødirektoratet (paginated, ~2700 reserves) |

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

   app.jwt.secret=your_jwt_secret
   ```

3. Build and run:
   ```bash
   ./mvnw spring-boot:run
   ```

   The server starts on port `8080`. National parks and nature reserves are fetched from Miljødirektoratet automatically on first startup.

### Android emulator
The emulator reaches the host machine's localhost at `http://10.0.2.2:8080/`.

### Physical Android device
The device must be on the same Wi-Fi as the PC. Use the PC's LAN IP, e.g. `http://192.168.0.207:8080/`. Allow inbound TCP port 8080 in Windows Firewall.
