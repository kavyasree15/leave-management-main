# TechNova Portal: Advanced Business Logic Features for Leave Endpoints

These advanced features focus on enhancing the security, validation, and flexibility of the core **Medical Leaves** and **Paid Leaves** endpoints.

---

## 1. Advanced Medical Leaves Logic

### 1.1 Anti-Loophole Validation (Back-to-Back Leaves)
* **The Problem:** The current rule is: *"Medical leaves exceeding 3 days require a medical certificate"*. Employees can bypass this by filing two separate, consecutive 2-day leave requests (e.g., Request 1: Mon-Tue; Request 2: Wed-Thu).
* **The Logic:** When applying for a medical leave, the backend queries the database for any of the employee's existing medical leave requests that are adjacent to the new request (e.g., ending 1 day before the new start date, or starting 1 day after the new end date).
  * If the combined duration of the adjacent requests plus the new request exceeds 3 days, the system **rejects** the request unless a medical certificate is uploaded.

### 1.2 Automated OCR Medical Certificate Scanner (Mock AI Integration)
* **The Logic:** When a medical certificate is uploaded as a Base64 string, the backend does a validation scan:
  * Verifies the document is not an empty or invalid PDF.
  * (Optional API integration) Scans the text for critical keywords (e.g., "Doctor", "Medical Certificate", "Rest") and matches the date range written in the certificate with the `startDate` and `endDate` in the database.

---

## 2. Advanced Paid Leaves Logic

### 2.1 Corporate Blackout Dates & Team Quotas Check
* **The Logic:** 
  * **Blackout Dates:** The system maintains a list of corporate blackout dates (e.g., year-end financial audits or major product releases). If an employee applies for paid leave during a blackout date, the request is blocked or flagged for mandatory HR approval.
  * **Department Quota:** When applying, the service checks the department size. If more than **15%** of the team is already approved for leave during that period, the request is flagged with a warning: *"High absenteeism for department during this period; approval subject to manager discretion."*

### 2.2 Holiday & Weekend Exclusion (Dynamic Days Calculation)
* **The Problem:** Currently, applying for a leave from Friday to Monday (4 calendar days) deducts 4 days from the leave balance.
* **The Logic:** Integrate a **Holiday Calendar** database table. When calculating the deduction:
  * Exclude Saturdays and Sundays.
  * Exclude registered public company holidays.
  * *Result:* A leave request from Friday to Monday will only deduct **2 days** (Friday & Monday) instead of 4.

### 2.3 Loss of Pay (LOP) / Negative Balance Fallback
* **The Logic:** If an employee applies for paid leave but has insufficient balance:
  * Instead of a hard rejection, the system allows the employee to proceed and automatically converts the excess days into **Loss of Pay (LOP)** or **Unpaid Leave**.
  * Alternatively, support a **Negative Balance Allowance** (up to -5 days) which will be paid back when new leaves accrue in the next calendar year.
