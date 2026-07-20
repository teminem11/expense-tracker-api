# AWS deployment blueprint

This document describes a production-oriented deployment design for the portfolio project. The AWS application infrastructure is **not currently provisioned**, so the repository does not claim a live AWS deployment.

The manual `.github/workflows/deploy-aws.yml` workflow is prepared to deploy the API after an ECS/Fargate service, ECR repository, private networking and runtime secrets have been configured. It uses GitHub OIDC and short-lived AWS credentials rather than stored access keys.

## Recommended topology

- Application Load Balancer in public subnets with an ACM HTTPS certificate
- ECS/Fargate tasks in private subnets
- PostgreSQL RDS in private database subnets
- Private S3 bucket for receipts
- Secrets Manager entries referenced directly by the ECS task definition
- CloudWatch Logs for application output
- ECR repository for immutable application images

Do not assign public IP addresses to ECS tasks or RDS. Allow inbound traffic to the ECS security group only from the ALB security group, and to RDS only from the ECS security group.

## GitHub production environment

Create a GitHub environment named `production`, protect it with required reviewers, and add these environment variables:

| Variable | Example |
|---|---|
| `AWS_REGION` | `eu-west-1` |
| `AWS_DEPLOY_ROLE_ARN` | `arn:aws:iam::123456789012:role/github-expense-api-deploy` |
| `ECR_REPOSITORY` | `expense-tracker-api` |
| `ECS_CLUSTER` | `expense-tracker-production` |
| `ECS_SERVICE` | `expense-tracker-api` |
| `ECS_TASK_FAMILY` | `expense-tracker-api` |
| `ECS_CONTAINER_NAME` | `api` |

No static AWS access key is required. The workflow requests short-lived credentials using GitHub OIDC.

## Runtime secrets

Configure the ECS task definition to inject these values from AWS Secrets Manager:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`

Configure these non-secret environment variables directly in the task definition:

- `STORAGE_PROVIDER=s3`
- `AWS_REGION=eu-west-1`
- `AWS_S3_BUCKET=<private-bucket-name>`

The task role needs only `s3:GetObject`, `s3:PutObject` and `s3:DeleteObject` for `arn:aws:s3:::<bucket>/receipts/*`, plus permissions required to emit logs when they are not handled by the execution role.

## OIDC trust scope

The deploy role trust policy should accept GitHub's OIDC provider only when both conditions match:

- audience is `sts.amazonaws.com`;
- subject is the repository's protected `production` environment.

Use a subject in this form:

```text
repo:<github-owner>/<repository>:environment:production
```

The deploy role should be limited to pushing into the one ECR repository, reading/registering the one ECS task family, updating the one ECS service, and passing only the ECS execution/task roles used by that task definition.

## Deployment

After the infrastructure, GitHub environment and OIDC role exist, run the `Deploy to AWS ECS` workflow manually. It builds an image tagged with the immutable Git commit SHA, pushes it to ECR, registers a new task-definition revision and waits for ECS service stability.
