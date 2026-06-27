# TechNova Leave & Attendance Portal: Startup & Testing Runbook

This guide describes how to run and test all microservices in the TechNova Leave & Attendance Portal.

---

## 1. Prerequisites & Services Startup

Make sure your local infrastructure services are running:
* **MySQL Database:** Must be running on port `3306` (with credentials `root` / `root`, creates database `leave_mgmt` automatically).
* **Apache Kafka:** Must be running on port `9092` with topic `notification-topic` created.

### 1.1 Starting the Services (Order is Automatic)
Run the PowerShell script at the root directory of the project:
```powershell
./start-services.ps1
```
This script opens separate terminal windows and starts the services in the mandatory sequence:
1. **Config Server** (Port `8888`)
2. **Eureka Service Registry** (Port `8761`) — *Access Dashboard at http://localhost:8761/*
3. **API Gateway** (Port `8080`) — *All client requests are routed through here*
4. **Auth Service** (Port `8081`)
5. **Attendance Service** (Port `8082`)
6. **Leave Service** (Port `8083`)
7. **HR Service** (Port `8084`)
8. **Notification Service** (Port `8085`)

*Wait 15-20 seconds after starting for all services to register themselves on Eureka before sending API requests.*

---

## 2. Chronological Testing Flow & Endpoint Reference

All endpoints are accessed via the **API Gateway** on Port `8080`.

### Phase 1: Registration & Admin Approval (User Setup)

#### 1. Register Manager
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/auth/register`
* **Purpose:** Registers a Manager user.
* **Payload:**
  ```json
  {
      "username": "bhanu_manager",
      "password": "manager123",
      "role": "MANAGER",
      "managerId": null,
      "email": "manager@technova.com"
  }
  ```
* **Validation Rules:** `managerId` must be `null` for managers. `email` must be unique. Accounts start as unapproved.

#### 2. Register Employee
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/auth/register`
* **Purpose:** Registers an Employee reporting to a manager.
* **Payload:**
  ```json
  {
      "username": "bhanu_employee",
      "password": "employee123",
      "role": "EMPLOYEE",
      "managerId": 1,
      "email": "employee@technova.com"
  }
  ```
* **Validation Rules:** `managerId` is mandatory for employees and must point to a user with the `MANAGER` role.

#### 3. Log In as Admin
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/auth/login`
* **Purpose:** Authenticate the default administrator to approve new registrations.
* **Payload:**
  ```json
  {
      "email": "admin@technova.com",
      "password": "admin123"
  }
  ```
* **Response:** Returns an Admin JWT token.

#### 4. Approve Users
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/auth/approve/{id}` (e.g. `/approve/1`, `/approve/2`)
* **Headers:** `Authorization: Bearer <ADMIN_JWT_TOKEN>`
* **Purpose:** Admin approves the user account so they can log in. Returns `"approved"`.

---

### Phase 2: Login & Daily Attendance

#### 5. Log In as Employee
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/auth/login`
* **Purpose:** Logs in an employee to obtain their JWT token.
* **Payload:**
  ```json
  {
      "email": "employee@technova.com",
      "password": "employee123"
  }
  ```
* **Response:** Returns Employee JWT token. Use this token as `Bearer <TOKEN>` in the headers for all subsequent requests.

#### 6. Check In (Employee)
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/attendance/checkin`
* **Headers:** `Authorization: Bearer <EMPLOYEE_JWT_TOKEN>`
* **Purpose:** Registers check-in timestamp.
* **Business Logic:** 
  * Prevents double check-in on the same day.
  * If checked in after `09:15 AM`, automatically flags `late: true`.
  * Publishes check-in event to Kafka.

#### 7. Check Out (Employee)
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/attendance/checkout`
* **Headers:** `Authorization: Bearer <EMPLOYEE_JWT_TOKEN>`
* **Purpose:** Registers check-out timestamp.
* **Business Logic:** Calculates shift `workingHours` rounded to 2 decimal places and publishes check-out event to Kafka.

---

### Phase 3: Leave Management

#### 8. Check Leave Balance
* **HTTP Method:** `GET`
* **URL:** `http://localhost:8080/api/leaves/balance`
* **Headers:** `Authorization: Bearer <EMPLOYEE_JWT_TOKEN>`
* **Purpose:** Returns the employee's remaining leave credits (Casual: 10, Medical: 15, Paid: 20).

#### 9. Apply for Leave
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/leaves/apply`
* **Headers:** `Authorization: Bearer <EMPLOYEE_JWT_TOKEN>`
* **Purpose:** Submit a new leave request.
* **Payload (Casual Leave Example):**
  ```json
  {
      "startDate": "2026-07-01",
      "endDate": "2026-07-03",
      "leaveType": "CASUAL",
      "reason": "Family function",
      "managerId": 1
  }
  ```
* **Advanced Core Logics Handled:**
  * **Weekend Exclusion:** Excludes Saturdays and Sundays from balance deduction (e.g. Friday to Monday only deducts 2 days).
  * **Anti-Loophole Medical Certificate Check:** Sums up contiguous/back-to-back medical leave requests. If the combined chain exceeds 3 business days, it rejects the request unless `medicalCertificate` (Base64 PDF) is supplied.

---

### Phase 4: Manager & HR Approvals

#### 10. Get Pending Requests (Manager / HR)
* **HTTP Method:** `GET`
* **URL:** `http://localhost:8080/api/leaves/pending`
* **Headers:** `Authorization: Bearer <MANAGER_OR_HR_JWT_TOKEN>`
* **Purpose:** Fetches pending approvals.
  * If logged in as **Manager**: returns pending employee leave requests reporting to them.
  * If logged in as **HR**: returns pending leave requests that exceed 10 days.

#### 11. Approve Leave (Manager / HR)
* **HTTP Method:** `POST`
* **URL:** `http://localhost:8080/api/leaves/{requestId}/approve`
* **Headers:** `Authorization: Bearer <MANAGER_OR_HR_JWT_TOKEN>`
* **Purpose:** 
  * **Manager approves:** If duration <= 10 days, requests status updates to `APPROVED` and deducts the balance. If duration > 10 days, escalates request to `PENDING_HR`.
  * **HR approves:** Validates and approves requests > 10 days, updating status to `APPROVED` and deducting balance.
  * Prevents self-approval.

---

### Phase 5: HR Analytics & Reporting

#### 12. Get System HR Reports
* **HTTP Method:** `GET`
* **URL:** `http://localhost:8080/api/hr/reports`
* **Headers:** `Authorization: Bearer <HR_JWT_TOKEN>`
* **Purpose:** HR views the system summary report (Total employees, checked-in count today, late arrivals today, and approved leave count).
