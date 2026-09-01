# Monitoring URLs Guide

## Quick Access

Use these scripts to automatically get the correct URLs:

```bash
# Show all URLs
./scripts/open-all.sh

# Show only Kafka UI URL
./scripts/open-kafka-ui.sh

# Show only Dashboard URL
./scripts/open-dashboard.sh
```

---

## Running Locally (localhost)

```
🔗 Kafka UI:      http://localhost:8081
📊 Dashboard:     http://localhost:8080/metrics-dashboard.html
❤️ Health:        http://localhost:8080/actuator/health
📈 Metrics:       http://localhost:8080/actuator/metrics
```

---

## Running in GitHub Codespaces

Replace `CODESPACE_NAME` with your actual Codespace name (e.g., `psychic-pancake-wvgjw6v7w74f9vjw`):

```
🔗 Kafka UI:      https://CODESPACE_NAME-8081.app.github.dev
📊 Dashboard:     https://CODESPACE_NAME-8080.app.github.dev/metrics-dashboard.html
❤️ Health:        https://CODESPACE_NAME-8080.app.github.dev/actuator/health
📈 Metrics:       https://CODESPACE_NAME-8080.app.github.dev/actuator/metrics
```

### Finding Your Codespace Name

1. Run any command in Terminal: `echo $CODESPACE_NAME`
2. Or check the browser URL: `https://CODESPACE_NAME-PORT.app.github.dev`

---

## Service Ports

| Service | Port | Use |
|---------|------|-----|
| Spring Boot App | 8080 | API + Dashboard |
| Kafka UI | 8081 | Topic/Message Inspection |
| Kafka Broker | 9092 | Message Publishing (internal) |
| LocalStack | 4566 | AWS DynamoDB/SQS (internal) |

---

## Common Tasks

### 1. Publish a Transfer

```bash
./scripts/publish-transfer.sh tf-001 acc-123 acc-456 100.00
```

### 2. Check Transfer Status via API

```bash
# Replace CODESPACE_NAME or use localhost:8080
curl https://CODESPACE_NAME-8080.app.github.dev/api/v1/transfers/tf-001
```

### 3. View Kafka Messages

1. Open Kafka UI: `https://CODESPACE_NAME-8081.app.github.dev`
2. Click on "transfer-requested" topic
3. View message payloads in real-time

### 4. View Dashboard Metrics

1. Open Dashboard: `https://CODESPACE_NAME-8080.app.github.dev/metrics-dashboard.html`
2. Auto-refreshes every 2 seconds
3. Shows success/failure counts and processing time

### 5. Run All Tests

```bash
./gradlew test
```

---

## Troubleshooting

### "localhost not working in Codespaces"
✅ **Solution:** Use the Codespaces URL instead:
```
https://CODESPACE_NAME-8080.app.github.dev
```

### "Kafka UI showing no messages"
- Check that `./scripts/start-infra.sh` is running (Terminal 1)
- Wait 10 seconds for topic creation
- Refresh page

### "Dashboard not updating"
- Ensure `./scripts/start-app.sh` is running (Terminal 2)
- Check browser console for errors
- Try hard refresh (Ctrl+Shift+R)

### "Connection refused"
- Verify all services are running:
  ```bash
  docker ps
  ```
- Should show: kafka, kafka-ui, localstack containers

---

## Full Setup Example

**Terminal 1: Infrastructure**
```bash
./scripts/start-infra.sh
```

**Terminal 2: Application**
```bash
./scripts/start-app.sh
```

**Terminal 3: View URLs**
```bash
./scripts/open-all.sh
```

**Terminal 4: Publish Transfers**
```bash
./scripts/publish-transfer.sh tf-demo-001 acc-123 acc-456 250.00
```

**Browser:**
- Kafka UI: See the message appear in real-time
- Dashboard: Watch metrics update as messages are processed

---

## Notes

- Codespaces URLs are public by default; anyone with the link can access them
- URLs use HTTPS in Codespaces for security
- Kafka UI port is 8081 to avoid conflict with Spring Boot (8080)
- All services automatically sync with Codespaces networking
