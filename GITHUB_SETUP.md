# GitHub Setup Instructions

## Step 1: Initialize Git Repository Locally

```bash
cd /home/claude/bank-transfer-api
git init
git add .
git commit -m "chore: initial project setup with gradle, spring boot, kafka and dynamodb"
```

## Step 2: Create Repository on GitHub

1. Go to https://github.com/new
2. Fill in:
   - **Repository name**: `bank-transfer-api`
   - **Description**: Microservice for processing bank transfers with Kafka and DynamoDB
   - **Visibility**: Public (or Private if preferred)
   - **Initialize repository**: NO (we already have content)
   - Click "Create repository"

## Step 3: Add Remote and Push

```bash
git remote add origin https://github.com/danilocamargo5/bank-transfer-api.git
git branch -M main
git push -u origin main
```

## Step 4: Create Development Branch

```bash
git checkout -b develop
git push -u origin develop
```

## Step 5: Create First Feature Branch for PR #1

```bash
git checkout -b feature/pr-01-setup
# Make any adjustments if needed
git commit -am "feat: setup project structure and gradle configuration"
git push -u origin feature/pr-01-setup
```

## Step 6: Create Pull Request

1. Go to https://github.com/danilocamargo5/bank-transfer-api
2. Click "Compare & pull request" (should appear after push)
3. Fill in:
   - **Title**: `[PR #1] Project setup with Gradle, Spring Boot, Kafka and DynamoDB`
   - **Description**:
   ```
   ## Overview
   Initial setup of the bank-transfer-api microservice project.

   ## Changes
   - ✅ Spring Boot 3.2.4 with Java 21
   - ✅ Kotlin with Gradle DSL (build.gradle.kts)
   - ✅ Kafka and AWS DynamoDB dependencies
   - ✅ Docker Compose for local development (DynamoDB, Kafka, Zookeeper)
   - ✅ Application configuration (application.properties)
   - ✅ Project structure (domain, application, infrastructure, config)

   ## Testing
   ```bash
   make build
   make dev-up
   make run
   curl http://localhost:8080/actuator/health
   make dev-down
   ```

   ## Related Issues
   N/A (Initial setup)
   ```
   - **Base**: `develop`
   - **Compare**: `feature/pr-01-setup`
   - Click "Create pull request"

## Future PR Workflow

For each subsequent PR:

```bash
# Create feature branch from develop
git checkout develop
git pull origin develop
git checkout -b feature/pr-XX-description

# Make changes and commit
git add .
git commit -m "feat: description of changes"

# Push to GitHub
git push -u origin feature/pr-XX-description

# Create PR on GitHub (base: develop, compare: feature/pr-XX-description)
```

## Branch Strategy

- **main**: Production-ready code
- **develop**: Integration branch for features
- **feature/pr-XX-***: Individual feature branches

## Useful Commands

```bash
# View branches
git branch -a

# Switch branch
git checkout branch-name

# Create and switch to new branch
git checkout -b branch-name

# View commit history
git log --oneline

# View changes
git status
git diff

# Push current branch
git push -u origin branch-name
```
