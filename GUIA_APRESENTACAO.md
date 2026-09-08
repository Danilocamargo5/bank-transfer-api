# 🎤 Guia Rápido - Apresentação Bank Transfer API

## ⚡ EXECUÇÃO RÁPIDA (5 TERMINAIS)

### Terminal 0 (Setup Inicial)
```bash
./full-setup.sh
```
Aguarde até aparecer: "✅ SETUP COMPLETE!"

---

### Terminal 1 (App Principal)
```bash
./scripts/start-app.sh
```
Aguarde até aparecer: "Started BankTransferApiApplication"

---

### Terminal 2 (Coverage Server - JaCoCo)
```bash
./scripts/start-coverage-server.sh
```
Acesso:
```
http://localhost:8888
```

---

### Terminal 3 (Dashboard Server - Métricas)
```bash
./scripts/start-dashboard-server.sh
```
Acesso:
```
http://localhost:9999
```

---

### Terminal 4 (Kafka UI)
Já está rodando automaticamente:
```
http://localhost:8081
```

---

## 🔗 URLs DE ACESSO

| Serviço | URL | Porta |
|---------|-----|-------|
| Kafka UI | http://localhost:8081 | 8081 |
| Métricas Dashboard | http://localhost:9999 | 9999 |
| Coverage Report (JaCoCo) | http://localhost:8888 | 8888 |
| Spring Boot API | http://localhost:8080 | 8080 |
| Health Check | http://localhost:8080/actuator/health | 8080 |

---

## 🧪 TESTAR A APLICAÇÃO

### 1. Verificar se app está up
```bash
curl http://localhost:8080/actuator/health
```

### 2. Enviar mensagens de teste (30 transfers)
```bash
./scripts/DEMO.sh
```

### 3. Enviar mais mensagens de teste
```bash
./scripts/DEMO2.sh
```

### 4. Ver transfers criadas
```bash
curl http://localhost:8080/transfers
```

---

## 🔄 RESET - ZERAR TUDO E COMEÇAR DENOVO

### Opção 1: Passo a Passo (Controlar cada etapa)
```bash
# Parar tudo
Ctrl+C em todos os terminais
./scripts/stop-infra.sh

# Remover volumes (zera dados)
docker volume rm localstack-volume 2>/dev/null || true

# Limpar build (opcional)
./gradlew clean

# Recomeçar
./full-setup.sh
```

### Opção 2: Tudo junto (One-liner)
```bash
./scripts/stop-infra.sh && docker volume rm localstack-volume 2>/dev/null && ./full-setup.sh
```

---

## 📊 CENÁRIOS DE DEMONSTRAÇÃO

### Cenário 1: Happy Path
```bash
./scripts/DEMO.sh
# Vê mensagens sendo processadas no Kafka UI
# Vê métricas atualizando no Dashboard
# Vê coverage aumentando
```

### Cenário 2: Falha de Rede (Simular)
```bash
# Pausar container do DynamoDB
docker pause localstack

# Enviar mensagens (vão pra DLQ após 3 retries)
./scripts/DEMO2.sh

# Retomar
docker unpause localstack
```

### Cenário 3: Duplicata Detection
```bash
# Enviar mesma mensagem 2x
./scripts/publish-transfer.sh
./scripts/publish-transfer.sh
# Sistema detecta duplicata e não processa 2x
```

---

## 🛑 PARAR TUDO

```bash
# Parar containers
./scripts/stop-infra.sh

# Matar todos os servers HTTP
pkill -f "python3 -m http.server"
pkill -f "start-coverage-server"
pkill -f "start-dashboard-server"
```

---

## 💡 DICAS IMPORTANTES

1. **Não feche os terminais** durante a apresentação
2. **Abra as URLs em abas diferentes** do navegador
3. **Se algo falhar**, execute o reset e recomece
4. **Deixe rodando DEMO.sh** enquanto apresenta as telas
5. **Coverage e Dashboard** só funcionam se app estiver rodando
6. **Kafka UI** mostra mensagens em tempo real (melhor para demonstrar fluxo)

---

## 🆘 TROUBLESHOOTING

### App não sobe
```bash
# Verifica logs
docker logs bank-transfer-api

# Verifica se portas estão livres
lsof -i :8080
lsof -i :8081

# Mata processo antigo se necessário
pkill -f "java.*bank-transfer"
```

### Coverage/Dashboard não abre
```bash
# Verifica se Python está rodando
ps aux | grep "http.server"

# Verifica se porta está ocupada
lsof -i :8888
lsof -i :9999

# Manualmente criar servers
cd build/reports/jacoco/test/html
python3 -m http.server 8888
```

### Kafka não tem mensagens
```bash
# Re-publicar DEMO
./scripts/DEMO.sh

# Ver topics
docker exec -it localstack aws kafka list-topics --region us-east-1
```

---

**Última atualização:** 08/09/2026
**Autor:** Danilo Camargo
