#!/bin/bash

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=== Bank Transfer API - GitHub Push Script ===${NC}\n"

USERNAME="danilocamargo5"
REPO="bank-transfer-api"
REPO_URL="https://github.com/${USERNAME}/${REPO}.git"

echo -e "${YELLOW}This script will push your local repository to GitHub${NC}"
echo -e "${YELLOW}Repository: ${REPO_URL}${NC}\n"

# Check if repository exists (user needs to create it first)
echo -e "${BLUE}Step 1: Creating GitHub repository${NC}"
echo "Please create the repository manually at:"
echo "  https://github.com/new"
echo ""
echo "Repository details:"
echo "  - Name: bank-transfer-api"
echo "  - Description: Microservice for processing bank transfers with Kafka and DynamoDB"
echo "  - Visibility: Public (or Private)"
echo "  - ✅ Do NOT initialize with README, .gitignore, or license"
echo ""
read -p "Press ENTER after creating the repository on GitHub..."

# Get GitHub token
echo -e "\n${BLUE}Step 2: GitHub Authentication${NC}"
echo "You need a Personal Access Token (PAT) to push code."
echo ""
echo "To create a token:"
echo "  1. Go to: https://github.com/settings/tokens/new"
echo "  2. Set scopes: repo (full control of private repositories)"
echo "  3. Copy the token (you won't see it again!)"
echo ""
read -sp "Enter your GitHub Personal Access Token: " GITHUB_TOKEN
echo ""

if [ -z "$GITHUB_TOKEN" ]; then
    echo -e "${RED}❌ Token not provided. Aborting.${NC}"
    exit 1
fi

# Add remote
echo -e "\n${BLUE}Step 3: Adding GitHub remote${NC}"
cd /home/claude/bank-transfer-api

# Remove remote if it already exists
git remote remove origin 2>/dev/null

# Add remote with token (HTTPS)
git remote add origin "https://${USERNAME}:${GITHUB_TOKEN}@github.com/${USERNAME}/${REPO}.git"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Remote added successfully${NC}"
else
    echo -e "${RED}❌ Failed to add remote${NC}"
    exit 1
fi

# Push to main branch
echo -e "\n${BLUE}Step 4: Pushing to main branch${NC}"
git push -u origin main

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Main branch pushed successfully${NC}"
else
    echo -e "${RED}❌ Failed to push main branch${NC}"
    exit 1
fi

# Push to develop branch
echo -e "\n${BLUE}Step 5: Pushing to develop branch${NC}"
git push -u origin develop

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Develop branch pushed successfully${NC}"
else
    echo -e "${RED}❌ Failed to push develop branch${NC}"
    exit 1
fi

# Create feature branch for PR #1
echo -e "\n${BLUE}Step 6: Creating feature branch for PR #1${NC}"
git checkout -b feature/pr-01-setup

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Feature branch created${NC}"
else
    echo -e "${RED}❌ Failed to create feature branch${NC}"
    exit 1
fi

git push -u origin feature/pr-01-setup

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Feature branch pushed successfully${NC}"
else
    echo -e "${RED}❌ Failed to push feature branch${NC}"
    exit 1
fi

# Summary
echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}✅ All done! Your repository is ready!${NC}"
echo -e "${GREEN}========================================${NC}\n"

echo -e "${BLUE}Next steps:${NC}"
echo "1. Go to: https://github.com/${USERNAME}/${REPO}"
echo "2. You should see 3 branches: main, develop, feature/pr-01-setup"
echo "3. Create a Pull Request:"
echo "   - Base branch: develop"
echo "   - Compare: feature/pr-01-setup"
echo "   - Title: [PR #1] Project setup with Gradle, Spring Boot, Kafka and DynamoDB"
echo ""
echo -e "${BLUE}Or run this to create the PR via API:${NC}"
echo "  gh pr create --base develop --head feature/pr-01-setup --title '[PR #1] Project setup with Gradle, Spring Boot, Kafka and DynamoDB' --body 'Initial setup of bank-transfer-api microservice'"
echo ""
echo -e "${YELLOW}Security Note:${NC} The token has been used in this session only and not stored."
