# Runbook operational backend - versiune RO

**Versiune document:** RO (Romana)

## Pornire stack

```bash
docker compose up -d --build
```

## Verificare stare (health)

```bash
curl http://localhost:8000/api/health
curl http://localhost:8000/api/ready
```

## Vizualizare loguri

```bash
docker compose logs -f app
docker compose logs -f mysql
```

## Oprire stack

```bash
docker compose down
```

## Backup MySQL

```bash
docker exec constructiq-mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" procurement_db' > backup.sql
```

## Restore MySQL

```bash
docker exec -i constructiq-mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" procurement_db' < backup.sql
```

## Rollback aplicatie

1. Fa checkout la commit-ul/tag-ul anterior din git.
2. Reconstruieste si redeploy:

```bash
docker compose up -d --build app
```

3. Confirma readiness:

```bash
curl http://localhost:8000/api/ready
```
