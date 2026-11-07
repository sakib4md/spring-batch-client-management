# Credit Batch Backend

Spring Boot application that imports credit-client data from CSV into MySQL and performs scheduled batch processing.

## Requirements

- Java 25
- Maven
- MySQL running on `localhost:3306`
- Database named `credit_db`

Update the database username and password in `src/main/resources/application.properties`.

## Run

```powershell
mvn spring-boot:run
```

The backend runs at `http://localhost:8080`.

The scheduled batch job reads
`src/main/resources/data/synthetic_credit_clients_10000.csv` by default. Change
`credit.batch.csv-resource` in `application.properties` to use another classpath
or filesystem CSV.

Uploading a CSV replaces the entire `clients` table with the uploaded rows. The
demo scheduler that automatically imports and updates clients is disabled by
default. Set `credit.batch.scheduler.enabled=true` to enable it.

## API

- `GET http://localhost:8080/api/clients` — returns all imported clients.
- `POST http://localhost:8080/api/clients/import` — uploads a CSV file and upserts clients.

## CSV Import

The scheduler imports:

`E:/java/credit-batch/incoming/clients.csv`

Batch jobs are disabled at startup and run from the configured scheduler.
