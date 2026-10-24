# ResolveX — Complaint and Issue Tracking System

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20MVC-blue?style=for-the-badge)](#project-architecture)
[![Status](https://img.shields.io/badge/Status-Completed-success?style=for-the-badge)](#quick-start)

> **A high-performance, zero-dependency web dashboard for institutional complaint tracking and resolution priority scoring, built with plain Java.**  
> *No Tomcat, no external JARs, no external database, and no internet required.*

---

## 📌 Overview

**ResolveX** is an end-to-end complaint management and automated prioritization platform designed for colleges and educational institutions. It features **two independent web dashboards**:

1. **Admin Dashboard** (`http://localhost:8080`): Complete governance over section complaints, category tagging, severity assignment, team delegation, real-time status updates, and audit analytics.
2. **User / Student Dashboard** (`http://localhost:8081`): Section-isolated issue reporting with keyword category auto-detection, community vote support, and resolution confirmation.

---

## 🚀 Quick Start

### Option A: Double-Click Runner (Windows / macOS / Linux)

> **Prerequisite:** JDK 17 or newer installed and on `PATH`.

- **Windows:** Double-click `RUN_DEMO.bat`
- **Linux / macOS:** Run `./run_demo.sh`

Both servers will launch automatically:
- **Admin Dashboard:** [http://localhost:8080](http://localhost:8080)
- **User Dashboard:** [http://localhost:8081](http://localhost:8081)

---

### Option B: Eclipse IDE

1. Open **Eclipse IDE**.
2. Go to **File** &rarr; **Import...** &rarr; **General** &rarr; **Existing Projects into Workspace**.
3. Select the `ResolveX` directory and click **Finish**.
4. Right-click `AdminMain.java` &rarr; **Run As** &rarr; **Java Application**.
5. Right-click `UserMain.java` &rarr; **Run As** &rarr; **Java Application**.

---

## 🔐 Default Login Credentials

| Role | Username | Password | Access Scope |
| :--- | :--- | :--- | :--- |
| **System Administrator** | `admin` | `admin123` | Institutional Admin Dashboard (Port 8080) |
| **CSE Section Students** | `cse1` ... `cse50` | `pass123` | CSE Complaints & Voting (Port 8081) |
| **AIDS Section Students** | `aids1` ... `aids40` | `pass123` | AIDS Complaints & Voting (Port 8081) |
| **AIML Section Students** | `aiml1` ... `aiml10` | `pass123` | AIML Complaints & Voting (Port 8081) |

> **Security Note:** All passwords are automatically hashed with salted **SHA-256** before storage via `PasswordUtil.java`.

---

## 📊 Dataset Breakdown

All baseline numbers are deterministic and calibrated to guarantee consistent demonstrations:

| Section | Total Students | Student Usernames | Initial Complaints |
| :--- | :---: | :---: | :---: |
| **CSE** | 50 | `cse1` &ndash; `cse50` | 50 |
| **AIDS** | 40 | `aids1` &ndash; `aids40` | 40 |
| **AIML** | 10 | `aiml1` &ndash; `aiml10` | 10 |
| **Total** | **100** | &mdash; | **100** |

- **Section Isolation:** Students only view and vote on issues relevant to their section.
- **Fair Representation:** Votes are computed as a **percentage of section strength** rather than raw votes, preventing smaller sections (like AIML with 10 students) from being drowned out by larger cohorts.

---

## 🧮 Automated Priority Calculation Formula

ResolveX calculates dynamic urgency using a multi-factor weighting algorithm implemented in [`PriorityCalculator.java`](src/com/resolvex/service/PriorityCalculator.java):

$$\text{Support \%} = \frac{\text{Votes}}{\text{Section Strength}} \times 100$$

$$\text{Score} = (0.35 \times \text{Severity}) + (0.35 \times \text{Support \%}) + (0.20 \times \text{Urgency}) + (0.10 \times \text{Waiting Time})$$

### Factor Normalization:
- **Severity (1–5):** Scaled by $\times 20$ (0–100)
- **Urgency (1–5):** Scaled by $\times 20$ (0–100)
- **Support %:** Scaled (0–100)
- **Waiting Time:** $\min\left(100, \frac{\text{Days Elapsed} \times 100}{30}\right)$
- **Safety Critical:** Flagged issues preserve their mathematical score while automatically pinned to the top with high-visibility badges.

---

## 📁 Project Architecture

```
ResolveX/
├── .classpath                   # Eclipse classpath configuration
├── .project                     # Eclipse project metadata
├── README.md                    # Repository documentation
├── README.txt                   # Quick instructions & demo notes
├── RUN_DEMO.bat                 # 1-click Windows runner
├── run_demo.sh                  # 1-click Unix runner
├── RESET_DATA.bat               # Windows state reset tool
├── reset_data.sh                # Unix state reset tool
├── bin/                         # Compiled bytecode (.class)
├── data/                        # Persistent file datastore
│   ├── ResolveX_Data.xlsx       # Excel workbook with formulas & metrics
│   ├── accounts.txt             # User & Admin credentials (SHA-256)
│   ├── categories.txt           # Category taxonomy & keywords
│   ├── complaints.txt           # Master complaint register
│   ├── groups.txt               # Section rosters & strengths
│   └── import/                  # Source CSV initialization seeds
│       ├── students.csv         # 100 student seeds
│       └── complaints.csv       # 100 initial complaints
├── src/                         # Pure Java source code
│   └── com/resolvex/
│       ├── AdminMain.java       # Admin HTTP Server launcher (Port 8080)
│       ├── UserMain.java        # User HTTP Server launcher (Port 8081)
│       ├── model/               # POJO Domain Entities
│       │   ├── Account.java
│       │   ├── Category.java
│       │   ├── Complaint.java
│       │   └── StudentGroup.java
│       ├── service/             # Business Logic & Algorithms
│       │   ├── ComplaintManager.java
│       │   ├── ComplaintOperations.java
│       │   ├── DataStore.java
│       │   └── PriorityCalculator.java
│       ├── util/                # Utilities
│       │   └── PasswordUtil.java
│       └── web/                 # Built-in Lightweight HTTP Routing & Views
│           ├── AdminServer.java
│           ├── Layout.java
│           ├── Session.java
│           ├── UserServer.java
│           └── Web.java
└── web/
    └── style.css                # Clean, responsive CSS styling
```

---

## 🔄 Resetting Demo State

To restore all complaints, votes, and accounts back to their original factory demo state:
1. Stop both running servers (`AdminMain` and `UserMain`).
2. Run `RESET_DATA.bat` (Windows) or `./reset_data.sh` (Linux/Mac).
3. Restart the servers.
