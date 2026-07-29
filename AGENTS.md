# Repository Guidelines

## Project Structure & Module Organization

This repository is a Maven-based Spring Boot application targeting Java 26. Production code lives under `src/main/java/com/mc4/airun`; keep new packages beneath that namespace and group code by feature (for example, `chat`, `scheduler`, or `vectorstore`). Configuration and other classpath resources belong in `src/main/resources`, with shared settings currently in `application.properties`. Tests mirror the production package tree under `src/test/java`. Maven build output is generated in `target/` and must not be committed.

## Build, Test, and Development Commands

Use the checked-in Maven wrapper so builds use a consistent Maven version:

- `./mvnw spring-boot:run` starts the application locally with DevTools support.
- `./mvnw test` compiles the project and runs the test suite.
- `./mvnw clean verify` performs a clean build and all verification steps; run it before opening a pull request.
- `./mvnw package` creates the executable JAR in `target/`.

On Windows, use the equivalent `mvnw.cmd` commands. A Java 26 JDK is required.

## Coding Style & Naming Conventions

Follow standard Java and Spring conventions: four-column indentation, one public top-level type per file, and no wildcard imports. Use `PascalCase` for classes, `camelCase` for methods and fields, and lowercase package names. Name Spring components by responsibility, such as `JobScheduler`, `ChatController`, or `EmbeddingService`. Prefer constructor injection and small, focused classes. Use Java records for immutable data carriers when possible. For regular classes, prefer Lombok over handwritten boilerplate and choose aggregate annotations such as `@Value` instead of several single-purpose annotations. No formatter or linter is configured, so preserve the surrounding style and rely on IDE formatting.

## Testing Guidelines

Tests use JUnit Jupiter and Spring Boot’s test support. Name test classes `*Tests` and test methods after observable behavior, such as `createsJobForValidRequest`. Keep unit tests isolated; use `@SpringBootTest` only when the full application context is necessary. Add tests for new behavior and regressions, then run `./mvnw test`.

## Configuration & Security

Never commit API keys, database passwords, or other secrets. Supply OpenAI and PostgreSQL/PGvector credentials through environment variables or an ignored local profile. Keep safe defaults in `application.properties`.

## Commit & Pull Request Guidelines

Git history is not included in this checkout. Use concise, imperative Conventional Commit messages, for example `feat: add scheduled prompt execution` or `fix: handle missing embeddings`. Pull requests should explain the change, link relevant issues, identify configuration or schema changes, and include test results. Add screenshots or sample requests/responses when HTTP behavior changes.
