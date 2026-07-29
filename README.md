# Airun

Airun runs as three independent Spring Boot microservices:

- `airun-ingestion` monitors the training directory, reads stable FIT files,
  and publishes their raw content to Kafka;
- `airun-activities` parses raw FIT activities, persists trainings in
  PostgreSQL, and exposes them through REST;
- `airun-logging` consumes stored-activity notifications, fetches the complete
  training from `airun-activities`, and logs it.

There is no shared Java module. Each service owns its domain and message
contract. Kafka JSON and the activities REST API are the integration
boundaries. The default topics are `airun.activity.raw.v1` and
`airun.activity.stored.v1`.

## Run all services

```shell
docker compose up --build
```

Copy `.fit` files into `./trainings`. A file is processed after its size and
modification time remain unchanged for one polling interval.

```shell
cp example.fit trainings/
docker compose logs -f airun-ingestion airun-activities airun-logging
```

PostgreSQL data is kept in the `postgres-data` volume.
The activities API is available at
`GET http://localhost:8080/api/trainings` and
`GET http://localhost:8080/api/trainings/{sourceName}`.

## Run a service locally

Each module starts independently:

```shell
./mvnw -pl airun-ingestion spring-boot:run
./mvnw -pl airun-activities spring-boot:run
./mvnw -pl airun-logging spring-boot:run
```

Relevant environment variables:

- `SPRING_KAFKA_BOOTSTRAP_SERVERS`
- `AIRUN_RAW_ACTIVITY_TOPIC`
- `AIRUN_ACTIVITY_STORED_TOPIC`
- `AIRUN_INGESTION_DIRECTORY`
- `AIRUN_INGESTION_POLL_INTERVAL`
- `AIRUN_ACTIVITIES_BASE_URL`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`

## End-to-end tests

`airun-e2e` starts Kafka and PostgreSQL with Testcontainers, launches all
three applications independently, writes a generated FIT file, and verifies
the REST response and logging consumer:

```shell
./mvnw -pl airun-e2e -am test
```
