# StepSentry

**Evidence-based release readiness for software workflows.**

StepSentry is a full-stack application that helps teams check whether a release workflow has the required evidence before it is considered ready. Teams create workflows, define mandatory requirements, attach evidence such as pull requests and test reports, review submitted evidence, and check whether evidence is still relevant to the workflow's current version.

StepSentry is not a replacement for GitHub or Jira. It focuses on **evidence completeness, review status, version freshness, and explainable readiness checks**.

## Features

- **Workflow management:** Create and list release workflows with a target version.
- **Requirement management:** Add mandatory or optional requirements to each workflow.
- **Evidence management:** Submit evidence links with a type, description, and source version.
- **Evidence review:** Approve or reject submitted evidence.
- **Version-aware freshness:** Compare the workflow's current version with an evidence item's source version and identify potentially stale evidence.
- **Readiness assessment:** Show missing, stale, pending-review, and rejected requirements, with summary counts and requirement-level details.
- **Full-stack UI:** React dashboard for managing workflows, evidence, and readiness.
- **REST API:** Spring Boot endpoints that can also be explored with Postman.
- **Validation and error responses:** Validate requests and return structured API errors.
- **Automated tests:** Backend tests run using the Maven Wrapper and an in-memory H2 database.

## Technology stack

| Area | Technologies |
| --- | --- |
| Backend | Java 21, Spring Boot 3, Spring Web |
| Persistence | Spring Data JPA, Hibernate ORM |
| Database | MySQL |
| Frontend | React, JavaScript, Vite |
| UI | Bootstrap, Bootstrap Icons, CSS |
| API and build tools | REST APIs, Maven Wrapper, Postman |
| Version control | Git, GitHub |
| Testing | JUnit / Spring tests, H2 for tests |

## How it works

```text
React UI (Vite)
      |
      | HTTP / JSON
      v
Spring Boot REST API
      |
      v
Controller -> Service -> Spring Data JPA / Hibernate
      |
      v
    MySQL
```

The readiness service evaluates each workflow requirement against its evidence. Evidence with a source version that differs from the workflow's current version can be marked potentially stale. The backend returns the readiness result and a breakdown that the UI displays.

**Workflow lifecycle and readiness are different:** a workflow can still have lifecycle status `DRAFT` while its evidence checks report `READY FOR RELEASE`. Readiness indicates that the configured evidence rules are satisfied; it does not publish or release the workflow.

## Screens in the application

- **Overview:** Workflow totals, release-ready count, items awaiting review, blocked workflows, and release health.
- **Workflows:** Create workflows, manage requirements, submit evidence, and review pending evidence.
- **Evidence:** Browse evidence grouped by requirement and see review status and version freshness.
- **Readiness:** View the official backend readiness result, summary counts, and requirement-level findings.

## Prerequisites

- Java 21
- MySQL 8.x
- Node.js and npm
- Windows PowerShell (commands below use PowerShell)

The Maven Wrapper is included, so a separate Maven installation is not required.

## Setup and run locally

### 1. Create the MySQL database

Start MySQL, then create the database:

```sql
CREATE DATABASE stepsentry
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

### 2. Configure and start the backend

Open a PowerShell terminal in the repository root:

```powershell
cd C:\Users\Admin\Desktop\StepSentry
$env:STEPSENTRY_DB_USERNAME = "root"
$env:STEPSENTRY_DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
.\mvnw.cmd spring-boot:run
```

Replace `YOUR_MYSQL_PASSWORD` with your local MySQL password. Keep credentials in environment variables or local configuration; do not commit passwords.

The backend runs at `http://localhost:8080` by default.

### 3. Start the frontend

Open a **second** PowerShell terminal:

```powershell
cd C:\Users\Admin\Desktop\StepSentry\stepsentry-ui
npm install
npm run dev
```

Open the local URL printed by Vite, usually `http://localhost:5173`.

The frontend is configured to call the backend at `http://localhost:8080`. CORS is configured for the local Vite development origin.

### 4. Run backend tests

From the repository root:

```powershell
.\mvnw.cmd test
```

The tests use an in-memory H2 database and do not require MySQL to be running. For local development, the project uses Hibernate schema updates; review database migration and schema settings before any production deployment.

## API overview

Base URL: `http://localhost:8080/api`

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `GET` | `/health` | Check application health |
| `GET` | `/workflows` | List workflows |
| `POST` | `/workflows` | Create a workflow |
| `GET` | `/workflows/{workflowId}` | Retrieve a workflow |
| `PATCH` / `PUT` | `/workflows/{workflowId}/version` | Update the workflow's target version |
| `GET` | `/workflows/{workflowId}/requirements` | List requirements |
| `POST` | `/workflows/{workflowId}/requirements` | Create a requirement |
| `GET` / `PUT` | `/workflows/{workflowId}/requirements/{requirementId}` | Retrieve or update a requirement |
| `GET` / `POST` | `/workflows/{workflowId}/requirements/{requirementId}/evidence` | List or submit evidence |
| `GET` / `DELETE` | `/workflows/{workflowId}/requirements/{requirementId}/evidence/{evidenceId}` | Retrieve or delete evidence |
| `PATCH` / `PUT` | `/workflows/{workflowId}/requirements/{requirementId}/evidence/{evidenceId}/review` | Approve or reject evidence |
| `GET` | `/workflows/{workflowId}/readiness` | Calculate readiness and evidence freshness |

For example, check application health with:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

Check readiness for workflow `1` with:

```powershell
Invoke-RestMethod http://localhost:8080/api/workflows/1/readiness
```

Evidence types include `PULL_REQUEST`, `TEST_REPORT`, `DOCUMENT`, and `OTHER`. Review states include `PENDING`, `APPROVED`, and `REJECTED`. Readiness responses include requirement and evidence counts, freshness classifications, and the reasons a workflow is not ready.

## Configuration

The backend supports configuration through environment variables:

| Variable | Default | Description |
| --- | --- | --- |
| `STEPSENTRY_DB_URL` | `jdbc:mysql://localhost:3306/stepsentry?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC connection URL |
| `STEPSENTRY_DB_USERNAME` | `root` | MySQL username |
| `STEPSENTRY_DB_PASSWORD` | empty | MySQL password |
| `STEPSENTRY_JPA_DDL_AUTO` | `update` | Hibernate schema mode; intended for local development |
| `SERVER_PORT` | `8080` | Backend HTTP port |

`.env.example` documents the variable names. Spring Boot does not automatically load a `.env` file by default; set variables in your shell or IDE run configuration.

## Repository structure

```text
StepSentry/
├── src/main/java/com/stepsentry/
│   ├── common/       # Shared API types and exception handling
│   ├── config/       # CORS configuration
│   ├── workflow/     # Workflows and requirements
│   ├── evidence/     # Evidence submission and review
│   └── readiness/    # Readiness calculation and API
├── src/test/         # Backend tests
├── stepsentry-ui/
│   ├── src/
│   │   ├── App.jsx
│   │   ├── App.css
│   │   └── main.jsx
│   ├── package.json
│   └── vite.config.js
├── .env.example
├── mvnw
├── mvnw.cmd
└── pom.xml
```

## Current scope and future improvements

The current project includes workflow and requirement management, evidence submission and review, version-aware freshness checks, readiness reporting, and a React UI.

Potential future enhancements include authentication and role-based access, real GitHub/Jira integrations, CI/CD evidence ingestion, database migrations, and deployment packaging. These are future improvements, not claims about existing functionality.

## Author

**Sanika Kulkarni**

