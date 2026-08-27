# ⚡ Quick Start - Push to GitHub

## 🎯 What You Have Now

Your local repository is ready with:
- ✅ Git initialized with initial commit
- ✅ `main` branch with all project files
- ✅ `develop` branch created
- ✅ All configuration files ready

## 📋 Step-by-Step Instructions

### Step 1: Create Repository on GitHub
Go to: https://github.com/new

Fill in:
- **Repository name**: `bank-transfer-api`
- **Description**: Microservice for processing bank transfers with Kafka and DynamoDB
- **Visibility**: Public (recommended)
- **Initialize**: ❌ NO - Do NOT initialize with README, .gitignore, or license

Click "Create repository"

### Step 2: Get GitHub Personal Access Token

Go to: https://github.com/settings/tokens/new

Settings:
- **Token name**: `bank-transfer-api-push`
- **Expiration**: 7 days (or more if you prefer)
- **Scopes**: Check `repo` (Full control of private repositories)

Click "Generate token" and copy it (you won't see it again!)

### Step 3: Run Push Script

Option A - Interactive Script (Recommended):
```bash
cd /home/claude/bank-transfer-api
chmod +x push-to-github.sh
./push-to-github.sh
```

The script will:
1. Ask you to confirm repository creation
2. Ask for your GitHub token (input is hidden)
3. Push `main` and `develop` branches
4. Create `feature/pr-01-setup` branch
5. Push all branches to GitHub

Option B - Manual Commands:
```bash
cd /home/claude/bank-transfer-api

# Replace YOUR_USERNAME and YOUR_TOKEN
USERNAME="danilocamargo5"
TOKEN="ghp_xxxxx" # Your GitHub token

# Add remote
git remote add origin https://${USERNAME}:${TOKEN}@github.com/${USERNAME}/bank-transfer-api.git

# Push main
git push -u origin main

# Push develop
git push -u origin develop

# Create and push feature branch
git checkout -b feature/pr-01-setup
git push -u origin feature/pr-01-setup
```

### Step 4: Create Pull Request

Go to: https://github.com/danilocamargo5/bank-transfer-api

You should see a "Compare & pull request" button.

Click it and fill:
- **Title**: `[PR #1] Project setup with Gradle, Spring Boot, Kafka and DynamoDB`
- **Base branch**: `develop` (important!)
- **Compare branch**: `feature/pr-01-setup`
- **Description**:

```markdown
## Overview
Initial setup of the bank-transfer-api microservice project.

## Changes
- ✅ Spring Boot 3.2.4 with Java 21
- ✅ Kotlin with Gradle DSL (build.gradle.kts)
- ✅ Kafka and AWS DynamoDB dependencies
- ✅ Docker Compose for local development
- ✅ Project structure (domain, application, infrastructure, config)
- ✅ Application configuration (externalized)

## How to Test
```bash
make build
make dev-up
make run
curl http://localhost:8080/actuator/health
make dev-down
```

## Checklist
- [x] Project builds successfully
- [x] Dependencies configured
- [x] Docker Compose works
- [x] README and documentation complete
```

Click "Create pull request"

---

## ✅ Verification

After pushing, verify on GitHub:
1. Go to: https://github.com/danilocamargo5/bank-transfer-api
2. Check branches:
   - `main` - should have 1 commit
   - `develop` - should have 1 commit
   - `feature/pr-01-setup` - should have 1 commit
3. Check files are present
4. Create and review the PR

---

## 🔒 Security Notes

- **Token**: Use PAT with limited scope and expiration
- **Don't commit tokens**: They're not stored in git
- **Revoke after use**: Delete the token after pushing if using temporary one
- **Use SSH**: For production, consider SSH keys instead of tokens

---

## ❓ Troubleshooting

**"Remote already exists"**
```bash
git remote remove origin
# Then run push script again
```

**"Authentication failed"**
- Double-check your token is correct
- Ensure token has `repo` scope
- Try using `gh auth login` if GitHub CLI is installed

**"Repository not found"**
- Verify you created the repo on GitHub
- Check repository is public/private as intended
- Ensure username is correct: `danilocamargo5`

---

**Ready? Run the push script! 🚀**
