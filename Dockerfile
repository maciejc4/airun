FROM eclipse-temurin:26-jdk AS build

WORKDIR /workspace
COPY . .
ARG SERVICE_MODULE
RUN ./mvnw -pl "$SERVICE_MODULE" package -DskipTests \
    && cp "$SERVICE_MODULE"/target/"$SERVICE_MODULE"-*-exec.jar /workspace/app.jar

FROM eclipse-temurin:26-jre

RUN useradd --system --create-home airun
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
RUN mkdir /data && chown airun:airun /data

USER airun
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
