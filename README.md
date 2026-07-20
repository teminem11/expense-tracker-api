# Expense Tracker API

Portfolio-ready REST API built with Java 21, Spring Boot, PostgreSQL, JWT, Docker, Flyway and AWS S3.

## Features

- Registration and stateless JWT authentication
- Per-user categories and expenses
- Pagination and expense-date filtering
- Receipt upload and authenticated download (maximum 5 MB)
- Local filesystem or private AWS S3 storage
- OpenAPI/Swagger UI and Actuator health endpoint
- Flyway-managed PostgreSQL schema

## Run locally

Prerequisites: Java 21+, Maven and Docker.

Start PostgreSQL:

```bash
docker compose up -d postgres
mvn spring-boot:run
```

Or build and run the API and PostgreSQL entirely in Docker:

```bash
docker compose --profile full up --build -d
```

Open:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`

Register through `POST /api/auth/register`, copy the returned token, click **Authorize** and paste the token. Swagger adds the `Bearer` prefix automatically.

## Main endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/auth/register` | Register and receive a JWT |
| POST | `/api/auth/login` | Log in and receive a JWT |
| GET, POST | `/api/categories` | List or create categories |
| DELETE | `/api/categories/{id}` | Delete an unused category |
| GET, POST | `/api/expenses` | List/filter or create expenses |
| GET, PUT, DELETE | `/api/expenses/{id}` | Read, update or delete an expense |
| POST | `/api/expenses/{id}/receipt` | Upload a receipt image |
| GET | `/api/expenses/{id}/receipt` | View/download the authenticated user's receipt |

Expense responses expose `receiptAvailable` and a protected `receiptUrl`; internal filesystem paths and S3 keys are never returned.

## Configuration

Copy `.env.example` to `.env` when running the full Docker profile. Important variables:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | local PostgreSQL URL | JDBC connection URL |
| `DB_USERNAME` / `DB_PASSWORD` | development credentials | Database credentials |
| `JWT_SECRET` | development-only value | Signing secret, at least 32 bytes |
| `JWT_EXPIRATION` | `86400000` | Token lifetime in milliseconds |
| `STORAGE_PROVIDER` | `local` | `local` or `s3` |
| `LOCAL_STORAGE_DIRECTORY` | `uploads` | Local receipt root |
| `AWS_REGION` | `eu-west-1` | S3 bucket region |
| `AWS_S3_BUCKET` | empty | Private receipt bucket |

Never use the default JWT secret or database password in a deployed environment.

## Tests and build

```bash
mvn test
mvn package
```

The tests cover JWT handling, expense ownership, update and receipt behavior, local-storage path safety, and an HTTP integration flow using an isolated H2 database.

## AWS deployment notes

Set `STORAGE_PROVIDER=s3`, `AWS_REGION` and `AWS_S3_BUCKET`. Use an IAM instance/task role rather than static AWS keys. The role needs `s3:PutObject`, `s3:GetObject` and `s3:DeleteObject` only for the bucket's `receipts/*` prefix.

For production, run PostgreSQL in private RDS subnets, keep the S3 bucket private, terminate HTTPS at an Application Load Balancer, store secrets in AWS Secrets Manager or SSM Parameter Store, and ship application logs to CloudWatch.

The repository includes GitHub Actions for CI and an OIDC-based ECS deployment workflow. See [`docs/AWS_DEPLOYMENT.md`](docs/AWS_DEPLOYMENT.md) for the required AWS and GitHub configuration.

## Stop and reset

```bash
docker compose --profile full down
```

To also delete local database and receipt volumes:

```bash
docker compose --profile full down -v
```

The `-v` command permanently removes local project data.
