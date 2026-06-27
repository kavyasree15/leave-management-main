# TechNova Leave & Attendance Portal: Advanced Features Proposal

To transition the portal from a core functional tracker to an enterprise-grade solution that scales for 2,500+ employees, we can implement several advanced features. These features leverage our microservices architecture (Kafka events, separate database, centralized configuration) to automate manual operations.

---

## 1. Automated Monthly Accrual Engine (Scheduled Job)
* **What it is:** Instead of having static leave balances, a scheduled job (run on the 1st of every month at midnight) automatically accrues leave credits.
* **Implementation:** A Spring `@Scheduled(cron = "0 0 0 1 * *")` task in `leave-service` that increments the `casualLeave`, `medicalLeave`, and `paidLeave` balances of all active users in the database by a configured monthly accrual rate (e.g., +1.5 Casual, +1 Paid).
* **Business Benefit:** Eliminates the manual work done by HR to reset or credit leaves at the beginning of the month/year.

---

## 2. Geofencing & IP-Restricted Check-In
* **What it is:** Prevents employees from checking in when they are not physically at the office or connected to the office network.
* **Implementation:**
  * **IP Whitelisting:** The Gateway compares the client's source IP address against a list of corporate VPN / office IP ranges.
  * **Geofencing (GPS Coordinates):** Check-in payload takes `latitude` and `longitude`. The backend uses the Haversine formula to ensure the user is within a 100-meter radius of TechNova's office coordinates.
* **Business Benefit:** Eliminates "proxy check-ins" and ensures integrity of remote vs. on-premise attendance reporting.

---

## 3. Real-Time Kafka-Driven Slack / Email Alerts
* **What it is:** Sends instant notifications to managers when a critical event occurs (e.g. employee check-in is late, or a medical leave is pending approval).
* **Implementation:** The `notification-service` consumes events from the Kafka `notification-topic` and uses Spring Boot Mail (`JavaMailSender`) or standard Webhooks to post directly to Slack or send email alerts.
* **Business Benefit:** Speeds up approvals and alerts managers immediately about operational staffing gaps.

---

## 4. AI-Driven Absenteeism & Anomaly Detection
* **What it is:** Identifies patterns of leave abuse or unusual attendance behaviors (e.g. employees taking sick leaves consistently on Mondays/Fridays, or late check-ins occurring repeatedly on specific weekdays).
* **Implementation:**
  * An analytics scheduler in `attendance-service` analyzes the 30-day moving window of attendance records.
  * If an employee has >3 late check-ins in a week, or a pattern of Friday-to-Monday leaves, the system flags the employee record.
  * This is visualized in a **Manager/HR Anomaly Dashboard**.
* **Business Benefit:** Promotes transparency and helps HR spot potential burnout or policy abuse.

---

## 5. Half-Day & Hourly Permission Requests
* **What it is:** Support for non-standard leave durations such as "First Half (Morning) Leave", "Second Half (Afternoon) Leave", or "Hourly Permissions" (e.g., leaving 2 hours early for a doctor appointment).
* **Implementation:** Update `LeaveRequest` with a `duration` type enum (`FULL_DAY`, `FIRST_HALF`, `SECOND_HALF`, `HOURLY`) and adjust leave balance calculations by `0.5` credits or hours.
* **Business Benefit:** Crucial for white-collar office operations where employees don't want to forfeit an entire day's leave balance for short tasks.

---

## 6. HR Reporting Aggregators with PDF/Excel Export
* **What it is:** Automates the creation of compliance reports and payroll reconciliation sheets.
* **Implementation:** An exporter bean in `hr-service` that queries database views and compiles monthly summaries into downloadable `.xlsx` or `.pdf` files.
* **Business Benefit:** Fully eliminates the remaining "7 days to under 1 day" reconciliation effort. Payroll admins can click one button to download the finalized payroll-ready spreadsheet.
