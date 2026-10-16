# StepSentry

Workflow Evidence & Change Assurance Platform.

StepSentry helps software teams decide whether a workflow is ready to complete. Teams attach required evidence (for example a pull request link or a test report) and required approvals. If something is missing, completion is blocked. Later phases will also flag evidence that may be stale after the related code version changes.

This is not a replacement for GitHub or Jira. The long-term differentiator is **version-aware evidence** and **explainable readiness checks**.

Phase 1 is the project foundation only: a Spring Boot modular monolith, MySQL configuration, a health endpoint, error handling, and tests.

## Architecture (simple language)

Think of the application as one Java process with clear folders (packages), not as many microservices.

```
Browser / Postman
        |
        v
HealthController  (receives HTTP)
        |
        v
JSON response
```

A workflow or requirement request follows this path:

```
HTTP request
  -> Controller   (talks HTTP, validates input)
  -> Service      (business rules)
  -> Repository   (loads and saves data)
  -> MySQL (or H2 during tests)
```

Shared pieces live under `common` so workflow, evidence, and approval features can reuse the same error format.

## Package structure

```
src/main/java/com/stepsentry/
  StepSentryApplication.java     Starts Spring Boot
  common/
    api/                         HTTP DTOs and the health endpoint
    exception/                   Consistent JSON error responses
  workflow/
    Workflow.java                JPA entity mapped to workflows
    WorkflowStatus.java          DRAFT / ACTIVE
    WorkflowRepository.java      Database access
    WorkflowService.java         Create / list / get rules
    WorkflowController.java      REST endpoints
    WorkflowRequirement.java     Required steps on a workflow
    WorkflowRequirementRepository.java
    WorkflowRequirementService.java
    WorkflowRequirementController.java
    api/                         Request and response JSON types
  evidence/
    Evidence.java                Submitted proof for a requirement
    EvidenceType.java            PULL_REQUEST / TEST_REPORT / DOCUMENT / OTHER
    EvidenceRepository.java
    EvidenceService.java
    EvidenceController.java
    RequirementEvidenceCount.java
    api/                         Request and response JSON types
  readiness/
    WorkflowReadinessController.java
    WorkflowReadinessService.java
    api/                         Readiness response and summary records
```

| Package | Purpose |
| --- | --- |
| `com.stepsentry` | Application entry point |
| `com.stepsentry.common.api` | Shared HTTP request/response types and health |
| `com.stepsentry.common.exception` | Global exception handling |
| `com.stepsentry.workflow` | Workflow entities, persistence, and REST API |
| `com.stepsentry.evidence` | Evidence attached to a workflow requirement |
| `com.stepsentry.readiness` | Workflow readiness calculation and REST API |

Planned later (not created yet):

| Package | Purpose |
| --- | --- |
| `com.stepsentry.approval` | Recorded approvals |
| `com.stepsentry.github` | GitHub integration (later) |

## Data model

Implemented now:

| Table | Why it exists |
| --- | --- |
| `workflows` | One tracked process, such as "Release 1.2" or "PR #42" |
| `workflow_requirements` | Required steps that must be completed before a workflow is ready |
| `evidence` | Submitted proof for a requirement, including an optional source version |

Still later:

| Table | Why it exists |
| --- | --- |
| `approvals` | Who approved, in which role, and when |
| `users` | People who submit evidence or approvals (after authentication) |

Relationships:

```
workflows 1 ---- * workflow_requirements
workflow_requirements 1 ---- * evidence
workflows 1 ---- * approvals
users     1 ---- * evidence
users     1 ---- * approvals
```

`evidence` will store a `source_version` (for example a Git SHA) so a later freshness engine can mark tests outdated when the code changes.

## Prerequisites

- Java 21
- Maven Wrapper is included (`mvnw.cmd` on Windows). You do not need a global Maven install.
- MySQL 8.x **to run the application**
- Tests do **not** require MySQL

Create an empty database before starting the app:

```sql
CREATE DATABASE stepsentry CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## Configuration

Credentials come from environment variables. Do not put passwords in Git.

| Variable | Default | Meaning |
| --- | --- | --- |
| `STEPSENTRY_DB_URL` | `jdbc:mysql://localhost:3306/stepsentry?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC URL |
| `STEPSENTRY_DB_USERNAME` | `root` | Database user |
| `STEPSENTRY_DB_PASSWORD` | empty | Database password |
| `STEPSENTRY_JPA_DDL_AUTO` | `update` | Hibernate schema mode. `update` is for local development only. Use `none` or `validate` before any shared/production deploy. |
| `SERVER_PORT` | `8080` | HTTP port |

PowerShell example:

```powershell
$env:STEPSENTRY_DB_USERNAME = "root"
$env:STEPSENTRY_DB_PASSWORD = "your-local-password"
```

`.env.example` lists the same names. Spring Boot does not load `.env` files automatically. Copy the values into your shell, IDE run configuration, or a gitignored `application-local.yml` if you add one later.

## Run the application

From the project root:

```powershell
.\mvnw.cmd spring-boot:run
```

If MySQL is not running, or the database/user/password is wrong, startup will fail when Spring tries to open a connection. Fix MySQL first, then run again.

## Run tests

```powershell
.\mvnw.cmd test
```

Tests use the `test` profile and an in-memory H2 database (`create-drop`). You do not need MySQL to run tests.

## Try the health endpoint

After the app starts:

- Browser: open [http://localhost:8080/api/health](http://localhost:8080/api/health)
- PowerShell:

```powershell
Invoke-RestMethod http://localhost:8080/api/health
```

- Postman: `GET http://localhost:8080/api/health`

Expected JSON:

```json
{
  "status": "UP",
  "application": "StepSentry"
}
```

An unknown URL such as `GET http://localhost:8080/api/does-not-exist` returns a JSON error with HTTP 404.

## Workflow API

New workflows start in `DRAFT` status.

### Create a workflow

`POST http://localhost:8080/api/workflows`

Request:

```json
{
  "name": "Release 1.2",
  "description": "Verify evidence before release",
  "currentVersion": "v1.2.0"
}
```

`currentVersion` is optional (e.g. a Git commit SHA or semver string).

Response `201 Created`:

```json
{
  "id": 1,
  "name": "Release 1.2",
  "description": "Verify evidence before release",
  "status": "DRAFT",
  "currentVersion": "v1.2.0",
  "createdAt": "2026-10-15T18:00:00Z",
  "updatedAt": "2026-10-15T18:00:00Z"
}
```

A blank or missing `name` returns `400` with the existing error JSON, including `"name: name is required"` in `details`.

### List workflows

`GET http://localhost:8080/api/workflows`

Response `200 OK`:

```json
[
  {
    "id": 1,
    "name": "Release 1.2",
    "description": "Verify evidence before release",
    "status": "DRAFT",
    "currentVersion": "v1.2.0",
    "createdAt": "2026-10-15T18:00:00Z",
    "updatedAt": "2026-10-15T18:00:00Z"
  }
]
```

### Get one workflow

`GET http://localhost:8080/api/workflows/1`

A missing id returns `404`:

```json
{
  "timestamp": "2026-10-15T18:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Workflow not found: 1",
  "path": "/api/workflows/1",
  "details": []
}
```

### Update workflow version

When the codebase advances to a new commit or release tag, update the workflow's target version:

`PATCH http://localhost:8080/api/workflows/1/version` (or `PUT`)

Request:

```json
{
  "currentVersion": "v2.0.0"
}
```

Response `200 OK`:

```json
{
  "id": 1,
  "name": "Release 1.2",
  "description": "Verify evidence before release",
  "status": "DRAFT",
  "currentVersion": "v2.0.0",
  "createdAt": "2026-10-15T18:00:00Z",
  "updatedAt": "2026-10-16T12:00:00Z"
}
```

## Workflow requirements API

Requirements belong to one workflow. Local Hibernate `update` adds the `workflow_requirements` table without dropping existing workflow rows.

### Create a requirement

`POST http://localhost:8080/api/workflows/1/requirements`

Request:

```json
{
  "title": "Pull request link",
  "description": "Submit the GitHub PR URL",
  "mandatory": true,
  "sortOrder": 1
}
```

`mandatory` defaults to `true` and `sortOrder` defaults to `0` when omitted. Blank `title` or a negative `sortOrder` returns `400`.

Response `201 Created`:

```json
{
  "id": 1,
  "workflowId": 1,
  "title": "Pull request link",
  "description": "Submit the GitHub PR URL",
  "mandatory": true,
  "sortOrder": 1,
  "createdAt": "2026-10-15T18:00:00Z",
  "updatedAt": "2026-10-15T18:00:00Z"
}
```

### List requirements

`GET http://localhost:8080/api/workflows/1/requirements`

Items are ordered by `sortOrder` (then id). A missing workflow returns `404`.

### Get one requirement

`GET http://localhost:8080/api/workflows/1/requirements/1`

If the workflow is missing, the requirement is missing, or the requirement belongs to a different workflow, the API returns `404`.

### Update a requirement

`PUT http://localhost:8080/api/workflows/1/requirements/1`

```json
{
  "title": "Updated title",
  "description": "Now optional",
  "mandatory": false,
  "sortOrder": 5
}
```

Response `200 OK` uses the same JSON shape as create.

## Evidence API

Evidence represents submitted proof attached to a specific workflow requirement (such as a GitHub pull request link, a test run report, or a design document).

### Supported evidence types

| EvidenceType | Meaning |
| --- | --- |
| `PULL_REQUEST` | Link to a GitHub/GitLab pull or merge request |
| `TEST_REPORT` | Link to automated test results or CI run |
| `DOCUMENT` | Link to architectural or compliance documentation |
| `OTHER` | Any other supporting artifact URL |

### Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/workflows/{workflowId}/requirements/{requirementId}/evidence` | Attach new evidence to a requirement |
| `GET` | `/api/workflows/{workflowId}/requirements/{requirementId}/evidence` | List all evidence for a requirement |
| `GET` | `/api/workflows/{workflowId}/requirements/{requirementId}/evidence/{evidenceId}` | Retrieve a specific evidence item |
| `DELETE` | `/api/workflows/{workflowId}/requirements/{requirementId}/evidence/{evidenceId}` | Delete an evidence item |

### Create evidence

`POST http://localhost:8080/api/workflows/1/requirements/1/evidence`

Request:

```json
{
  "type": "PULL_REQUEST",
  "title": "Feature PR",
  "evidenceUrl": "https://github.com/example/repo/pull/42",
  "description": "Implements the release checklist",
  "sourceVersion": "abc123def"
}
```

Response `201 Created`:

```json
{
  "id": 1,
  "workflowId": 1,
  "requirementId": 1,
  "type": "PULL_REQUEST",
  "title": "Feature PR",
  "evidenceUrl": "https://github.com/example/repo/pull/42",
  "description": "Implements the release checklist",
  "sourceVersion": "abc123def",
  "createdAt": "2026-10-16T05:45:00Z",
  "updatedAt": "2026-10-16T05:45:00Z"
}
```

### List evidence

`GET http://localhost:8080/api/workflows/1/requirements/1/evidence`

Response `200 OK`:

```json
[
  {
    "id": 1,
    "workflowId": 1,
    "requirementId": 1,
    "type": "PULL_REQUEST",
    "title": "Feature PR",
    "evidenceUrl": "https://github.com/example/repo/pull/42",
    "description": "Implements the release checklist",
    "sourceVersion": "abc123def",
    "createdAt": "2026-10-16T05:45:00Z",
    "updatedAt": "2026-10-16T05:45:00Z"
  }
]
```

Evidence items are returned in creation order (`createdAt ASC, id ASC`).

### Retrieve one evidence item

`GET http://localhost:8080/api/workflows/1/requirements/1/evidence/1`

Response `200 OK`:

```json
{
  "id": 1,
  "workflowId": 1,
  "requirementId": 1,
  "type": "PULL_REQUEST",
  "title": "Feature PR",
  "evidenceUrl": "https://github.com/example/repo/pull/42",
  "description": "Implements the release checklist",
  "sourceVersion": "abc123def",
  "createdAt": "2026-10-16T05:45:00Z",
  "updatedAt": "2026-10-16T05:45:00Z"
}
```

### Delete evidence

`DELETE http://localhost:8080/api/workflows/1/requirements/1/evidence/1`

Response `204 No Content` (empty body).

### Validation rules and HTTP errors

- **Required fields**:
  - `type`: Must not be null and must match one of the defined `EvidenceType` values (`PULL_REQUEST`, `TEST_REPORT`, `DOCUMENT`, `OTHER`). Missing or invalid enum values return `400 Bad Request`.
  - `title`: Must not be blank. Empty or whitespace-only values return `400 Bad Request` with `"title: title is required"`.
  - `evidenceUrl`: Must not be blank and must be a valid absolute HTTP or HTTPS URL matching `^https?://\\S+$` (case-insensitive). Malformed URLs, blank values, and unsupported schemes (e.g., `ftp://`, `file://`, `javascript:`) return `400 Bad Request` with `"evidenceUrl: evidenceUrl must be an HTTP or HTTPS URL"`. Submitted URLs are never fetched or opened.
- **Optional fields**:
  - `description`: Optional text up to 2000 characters; whitespace is trimmed, and empty strings are stored as `null`.
  - `sourceVersion`: Optional text up to 100 characters (e.g., a Git commit SHA); whitespace is trimmed, and empty strings are stored as `null`.
- **Resource hierarchy and error conventions**:
  - All operations enforce strict hierarchical scoping (`Workflow -> Requirement -> Evidence`).
  - If the `workflowId` does not exist: `404 Not Found` with `"message": "Workflow not found: {workflowId}"`.
  - If the `requirementId` does not exist or does not belong to the specified workflow: `404 Not Found` with `"message": "Requirement not found: {requirementId}"`.
  - If the `evidenceId` does not exist or does not belong to the specified requirement: `404 Not Found` with `"message": "Evidence not found: {evidenceId}"`.

## Workflow readiness API

The readiness check evaluates whether a workflow is ready to complete, incorporating version-aware evidence freshness and stale evidence detection.

### Evidence freshness classifications

Evidence freshness is evaluated by comparing the workflow's `currentVersion` against the evidence item's `sourceVersion`:

| Status | Meaning | Condition |
| --- | --- | --- |
| `CURRENT` | Fresh evidence matching the target version | Both workflow `currentVersion` and evidence `sourceVersion` are present and match. |
| `POTENTIALLY_STALE` | Outdated evidence from an earlier or different commit | Both versions are present but differ. |
| `VERSION_UNAVAILABLE` | Version cannot be determined | Either workflow `currentVersion` or evidence `sourceVersion` is missing/omitted. |

### Readiness rules

- A workflow is **ready** (`ready: true`) when **every mandatory requirement** has at least one evidence item that is `CURRENT` or `VERSION_UNAVAILABLE`.
- If a mandatory requirement has evidence but **all** attached items are `POTENTIALLY_STALE`, the requirement blocks readiness and appears in `staleRequirements`.
- If a mandatory requirement has **no evidence** attached, it blocks readiness and appears in `missingRequirements`.
- If a requirement has **mixed current and stale evidence**, it is satisfied, but stale counts remain visible in the requirement's freshness breakdown.
- Non-mandatory (`mandatory: false`) requirements without evidence or with stale evidence do **not** block readiness.
- A workflow with zero requirements is considered **ready** (`ready: true`).
- **Dynamic recalculation**: Updating a workflow's version (e.g. via `PATCH /api/workflows/{id}/version`) immediately re-evaluates freshness across existing evidence on the next request without caching or deleting data.
- Missing workflows return `404 Not Found`.

### Check workflow readiness

`GET http://localhost:8080/api/workflows/1/readiness`

#### Example response when blocked by STALE evidence (`200 OK`)

Workflow is targeting commit `v2.0.0`, but QA test report was only run against `v1.0.0`:

```json
{
  "workflowId": 1,
  "status": "DRAFT",
  "workflowVersion": "v2.0.0",
  "ready": false,
  "summary": {
    "totalRequirements": 2,
    "mandatoryRequirements": 2,
    "satisfiedMandatoryRequirements": 1,
    "missingMandatoryRequirements": 0,
    "staleMandatoryRequirements": 1
  },
  "missingRequirements": [],
  "staleRequirements": [
    {
      "requirementId": 2,
      "title": "QA Signoff",
      "sortOrder": 2,
      "staleEvidenceCount": 1
    }
  ],
  "requirements": [
    {
      "requirementId": 1,
      "title": "PR Approval",
      "mandatory": true,
      "sortOrder": 1,
      "satisfied": true,
      "evidenceCount": 1,
      "freshness": "CURRENT",
      "currentEvidenceCount": 1,
      "staleEvidenceCount": 0,
      "versionUnavailableEvidenceCount": 0
    },
    {
      "requirementId": 2,
      "title": "QA Signoff",
      "mandatory": true,
      "sortOrder": 2,
      "satisfied": false,
      "evidenceCount": 1,
      "freshness": "POTENTIALLY_STALE",
      "currentEvidenceCount": 0,
      "staleEvidenceCount": 1,
      "versionUnavailableEvidenceCount": 0
    }
  ]
}
```

#### Example response when READY with mixed and fresh evidence (`200 OK`)

```json
{
  "workflowId": 1,
  "status": "DRAFT",
  "workflowVersion": "v2.0.0",
  "ready": true,
  "summary": {
    "totalRequirements": 2,
    "mandatoryRequirements": 1,
    "satisfiedMandatoryRequirements": 1,
    "missingMandatoryRequirements": 0,
    "staleMandatoryRequirements": 0
  },
  "missingRequirements": [],
  "staleRequirements": [],
  "requirements": [
    {
      "requirementId": 1,
      "title": "PR Approval",
      "mandatory": true,
      "sortOrder": 1,
      "satisfied": true,
      "evidenceCount": 2,
      "freshness": "CURRENT",
      "currentEvidenceCount": 1,
      "staleEvidenceCount": 1,
      "versionUnavailableEvidenceCount": 0
    },
    {
      "requirementId": 3,
      "title": "Optional Benchmark",
      "mandatory": false,
      "sortOrder": 2,
      "satisfied": false,
      "evidenceCount": 1,
      "freshness": "POTENTIALLY_STALE",
      "currentEvidenceCount": 0,
      "staleEvidenceCount": 1,
      "versionUnavailableEvidenceCount": 0
    }
  ]
}
```

## Phase status

Complete:

- Maven Spring Boot 3 modular-monolith skeleton
- MySQL configuration via environment variables
- `GET /api/health`
- Global JSON error handling
- Workflow create / list / get-by-id / update-version
- Workflow requirement create / list / get / update
- Evidence management create / list / get-by-id / delete
- Workflow readiness check (`GET /api/workflows/{workflowId}/readiness`)
- Evidence freshness & stale evidence detection (`CURRENT`, `POTENTIALLY_STALE`, `VERSION_UNAVAILABLE`)
- Health, workflow, requirement, evidence, and readiness tests

Not yet:

- Approval APIs
- Authentication / Spring Security
- GitHub integration
- React UI
- Docker
