# User Service API

This project provides RESTful APIs for managing users in a Spring Boot application, including user registration, login (JWT-based authentication), and basic CRUD operations.

## 📦 Tech Stack
- Java 17+
- Spring Boot
- Spring Security (JWT Authentication)
- MongoDB
- Swagger (OpenAPI 3.0)
- AWS S3 Logging

---

## 🚀 API Endpoints

### Authentication APIs

#### 1. User Sign Up
- **POST** `/api/auth/signup`
- **Description**: Registers a new user with username, email, password, and roles.
- **Security**: No authentication required.
- **Request Body**:
```json
{
  "username": "string",
  "email": "string",
  "password": "string",
  "role": ["user", "admin", "mod"]
}
Response:
{
  "message": "User registered successfully!"
}

#### 2. User Sign In
POST /api/auth/signin
Description: Authenticates user with username and password. Returns JWT token on success.
Security: No authentication required.
Request Body:
{
  "username": "string",
  "password": "string"
}

Response:
{
  "token": "JWT_TOKEN",
  "id": "userId",
  "username": "string",
  "email": "string",
  "roles": ["ROLE_USER"]
}


### User Management APIs (JWT Token Required)

> ⚠️ **All endpoints require JWT authentication.**  
Add your token in the `Authorization` header:

# JWT Authentication
After successful login, use the returned JWT token in the Authorization header for all protected endpoints:
Authorization: Bearer <JWT_TOKEN>

#### 3. Update User
PUT /api/auth/{id}
Description: Update an existing user's username and email.
Request Body:
{
  "username": "newUsername",
  "email": "newEmail@example.com"
}

Response: Updated user object

#### 4. Delete User
DELETE /api/auth/{id}
Description: Delete a user by ID.
Response:
"User deleted successfully"

#### 5. Fetches user data
GET /fetch-user-data
Description: Fetches user data from a predefined external API and saves it in the database.
External Source: `https://jsonplaceholder.typicode.com/users`
Response:
Users imported successfully from: https://jsonplaceholder.typicode.com/users


#### 6. Lists all users
GET /list-user-data
Description: Lists all users previously imported from the external API.
Response:
[
  {
    "id": "string",
    "username": "Bret",
    "email": "Sincere@april.biz",
    "roles": [ ... ]
  },
  ...
]

#### 7. Accepts a custom JSON URL
POST /save-json-data
Description: Accepts a custom JSON URL in the request body and imports unstructured data into the database.
Request Body:
{
  "url": "https://example.com/data.json"
}
Response:
Users imported successfully from: https://example.com/data.json

Error Response (missing URL):
Missing 'url' in request body


#### 8. Lists all unstructured
GET /list-json-data
Description: Lists all unstructured raw JSON data previously imported.
Response:
[
  {
    "id": 1,
    "name": "Sample Name",
    "address": {
      "street": "Example Street",
      "city": "Sample City"
    }
  },
  ...
]

# Log Simulation API (/api/log)
#### 9. Simulates logs
GET /simulate
Description: Simulates logs (request, transaction, error) and uploads them to S3.
Access: Authenticated users only
Sample Output (in S3):
INFO: API Request received at /api/test/simulate
INFO: DB Transaction - User fetched from DB
ERROR: Division by zero - / by zero


# 🪵 AWS S3 Logging
All API requests, DB transactions, and errors are logged and uploaded to an AWS S3 bucket for auditing and debugging purposes.

# MongoDB Configuration
properties
spring.data.mongodb.uri=mongodb://localhost:27017/*************
spring.data.mongodb.auto-index-creation=true

Sets the MongoDB connection URI (replace ************* with your actual DB name).
Enables automatic index creation on MongoDB collections.

# JWT Configuration
properties
userservice.app.jwtSecret= ******************
userservice.app.jwtExpirationMs=86400000

userservice.app.jwtSecret: Secret key used to sign JWT tokens.
userservice.app.jwtExpirationMs: Token expiration duration in milliseconds (e.g., 86400000 = 24 hours).

# IAM Role-Based Permissions
properties
app.roles.logger.permissions=read,write
app.roles.moderator.permissions=read
app.roles.editor.permissions=read,write,create,delete

Defines fine-grained access control for various user roles.
Each role is mapped to a set of allowed permissions:
logger: read, write
moderator: read
editor: read, write, create, delete

# AWS Credentials and S3 Configuration
properties
aws.accessKey=*************
aws.secretKey=**************
aws.region=us-east-1
aws.s3.bucket-name=*********

AWS IAM credentials and configuration for accessing AWS services like S3.
Set appropriate values or use IAM roles/environment variables in production instead of hardcoding.

# Swagger/OpenAPI Documentation
properties
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.display-request-duration=true
springdoc.swagger-ui.try-it-out-enabled=true
springdoc.api-docs.path=/v3/api-docs

# 📚 Swagger Documentation
Swagger UI: http://localhost:8080/swagger-ui.html

Project Structure (Simplified)
com.wareable.userservice
│
├── controller             → Handles incoming HTTP requests (e.g., AuthController, ExternalUserController)
│
├── exception              → Global exception handling (e.g., ValidationExceptionHandler)
│
├── logging                → S3 log uploader service (e.g., LogUploaderService)
│
├── model                  → Application domain models (e.g., AppUser, Role, ERole)
│
├── payload
│   ├── request            → DTOs for incoming requests (e.g., LoginRequest, SignupRequest)
│   └── response           → DTOs for outgoing responses (e.g., JwtResponse, MessageResponse)
│
├── repository             → MongoDB repositories (e.g., UserRepository, RoleRepository)
│
├── security
│   ├── jwt                → JWT token utilities and filters (e.g., JwtUtils, AuthTokenFilter)
│   └── config             → Security configuration classes (e.g., WebSecurityConfig)
│
├── service                → Service interfaces
│
└── service.impl           → Business logic and service implementations



