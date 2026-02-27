# Operations Runbook

## Start stack

```bash
docker compose up -d --build
```

## Verify health

```bash
curl http://localhost:8000/api/health
curl http://localhost:8000/api/ready
```

## View logs

```bash
docker compose logs -f app
docker compose logs -f mysql
```

## Stop stack

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

## Rollback app

1. Checkout previous git commit/tag.
2. Rebuild and redeploy:

```bash
docker compose up -d --build app
```

3. Confirm readiness:

```bash
curl http://localhost:8000/api/ready
```
