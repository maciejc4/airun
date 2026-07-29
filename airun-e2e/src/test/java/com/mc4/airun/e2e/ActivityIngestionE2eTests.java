package com.mc4.airun.e2e;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.garmin.fit.Activity;
import com.garmin.fit.ActivityMesg;
import com.garmin.fit.DateTime;
import com.garmin.fit.Event;
import com.garmin.fit.EventType;
import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;
import com.mc4.airun.activities.ActivitiesApplication;
import com.mc4.airun.ingestion.IngestionApplication;
import com.mc4.airun.logging.LoggingApplication;
import com.mc4.airun.logging.TrainingLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
class ActivityIngestionE2eTests {

    private static final String RAW_ACTIVITY_TOPIC = "airun.activity.raw.v1";
    private static final String ACTIVITY_STORED_TOPIC = "airun.activity.stored.v1";

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka-native:4.0.0")
    );

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer(
            DockerImageName.parse("postgres:17-alpine")
    );

    @TempDir
    Path monitoredDirectory;

    private final List<ConfigurableApplicationContext> applications = new ArrayList<>();

    @AfterEach
    void stopApplications() {
        applications.reversed().forEach(ConfigurableApplicationContext::close);
    }

    @Test
    void ingestsStoresExposesAndLogsFitActivity() throws IOException {
        ConfigurableApplicationContext activities = startActivities();
        int activitiesPort = Integer.parseInt(
                activities.getEnvironment().getRequiredProperty("local.server.port")
        );
        ListAppender<ILoggingEvent> logAppender = captureTrainingLogs();
        startLogging(activitiesPort);
        startIngestion();

        writeFitActivity(monitoredDirectory.resolve("morning-run.fit"));

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + activitiesPort + "/api/trainings/morning-run.fit"))
                .build();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body())
                    .contains("\"sourceName\":\"morning-run.fit\"")
                    .contains("\"sport\":\"RUNNING\"")
                    .contains("\"averageHeartRate\":140");
        });
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(logAppender.list)
                        .extracting(ILoggingEvent::getFormattedMessage)
                        .anyMatch(message -> message.contains("morning-run.fit"))
        );
    }

    private ConfigurableApplicationContext startActivities() {
        return start(
                ActivitiesApplication.class,
                "server.port=0",
                "spring.datasource.url=" + postgres.getJdbcUrl(),
                "spring.datasource.username=" + postgres.getUsername(),
                "spring.datasource.password=" + postgres.getPassword(),
                kafkaProperty(),
                groupProperty("activities"),
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JacksonJsonDeserializer",
                "spring.kafka.consumer.properties[spring.json.value.default.type]=com.mc4.airun.activities.events.RawActivity",
                "spring.kafka.consumer.properties[spring.json.trusted.packages]=com.mc4.airun.activities.events",
                "spring.kafka.consumer.properties[spring.json.use.type.headers]=false",
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer",
                "spring.kafka.producer.properties[spring.json.add.type.headers]=false",
                "airun.kafka.raw-activity-topic=" + RAW_ACTIVITY_TOPIC,
                "airun.kafka.activity-stored-topic=" + ACTIVITY_STORED_TOPIC
        );
    }

    private void startLogging(int activitiesPort) {
        start(
                LoggingApplication.class,
                kafkaProperty(),
                groupProperty("logging"),
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JacksonJsonDeserializer",
                "spring.kafka.consumer.properties[spring.json.value.default.type]=com.mc4.airun.logging.events.ActivityStored",
                "spring.kafka.consumer.properties[spring.json.trusted.packages]=com.mc4.airun.logging.events",
                "spring.kafka.consumer.properties[spring.json.use.type.headers]=false",
                "airun.kafka.activity-stored-topic=" + ACTIVITY_STORED_TOPIC,
                "airun.activities.base-url=http://localhost:" + activitiesPort
        );
    }

    private void startIngestion() {
        start(
                IngestionApplication.class,
                kafkaProperty(),
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JacksonJsonSerializer",
                "spring.kafka.producer.properties[spring.json.add.type.headers]=false",
                "airun.ingestion.directory=" + monitoredDirectory,
                "airun.ingestion.poll-interval=100ms",
                "airun.kafka.raw-activity-topic=" + RAW_ACTIVITY_TOPIC
        );
    }

    private ConfigurableApplicationContext start(Class<?> application, String... properties) {
        List<String> applicationProperties = new ArrayList<>(Arrays.asList(properties));
        boolean activitiesApplication = application.equals(ActivitiesApplication.class);
        if (!activitiesApplication) {
            applicationProperties.add("""
                    spring.autoconfigure.exclude=\
                    org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration,\
                    org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration\
                    """);
        }
        String[] arguments = applicationProperties.stream()
                .map(property -> "--" + property)
                .toArray(String[]::new);
        ConfigurableApplicationContext context = new SpringApplicationBuilder(application)
                .web(activitiesApplication ? WebApplicationType.SERVLET : WebApplicationType.NONE)
                .properties("logging.level.org.apache.kafka=WARN")
                .run(arguments);
        applications.add(context);
        return context;
    }

    private String kafkaProperty() {
        return "spring.kafka.bootstrap-servers=" + kafka.getBootstrapServers();
    }

    private String groupProperty(String service) {
        return "spring.kafka.consumer.group-id=e2e-" + service + "-" + UUID.randomUUID();
    }

    private ListAppender<ILoggingEvent> captureTrainingLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger(TrainingLogger.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void writeFitActivity(Path path) {
        DateTime startedAt = fitTime("2026-07-29T06:00:00Z");
        DateTime endedAt = fitTime("2026-07-29T06:10:00Z");
        FileEncoder encoder = new FileEncoder(path.toFile());

        FileIdMesg fileId = new FileIdMesg();
        fileId.setType(com.garmin.fit.File.ACTIVITY);
        fileId.setManufacturer(Manufacturer.DEVELOPMENT);
        fileId.setProduct(1);
        fileId.setSerialNumber(1L);
        fileId.setTimeCreated(startedAt);
        encoder.write(fileId);
        encoder.write(record(startedAt, (short) 120, 0));
        encoder.write(record(endedAt, (short) 160, 2_000));

        SessionMesg session = new SessionMesg();
        session.setMessageIndex(0);
        session.setTimestamp(endedAt);
        session.setEvent(Event.SESSION);
        session.setEventType(EventType.STOP);
        session.setStartTime(startedAt);
        session.setSport(Sport.RUNNING);
        session.setSubSport(SubSport.GENERIC);
        session.setTotalElapsedTime(600F);
        session.setTotalTimerTime(600F);
        session.setTotalDistance(2_000F);
        session.setAvgHeartRate((short) 140);
        session.setMaxHeartRate((short) 160);
        session.setFirstLapIndex(0);
        session.setNumLaps(0);
        encoder.write(session);

        ActivityMesg activity = new ActivityMesg();
        activity.setTimestamp(endedAt);
        activity.setTotalTimerTime(600F);
        activity.setNumSessions(1);
        activity.setType(Activity.MANUAL);
        activity.setEvent(Event.ACTIVITY);
        activity.setEventType(EventType.STOP);
        encoder.write(activity);
        encoder.close();
    }

    private DateTime fitTime(String instant) {
        return new DateTime(java.util.Date.from(Instant.parse(instant)));
    }

    private RecordMesg record(DateTime timestamp, short heartRate, float distance) {
        RecordMesg record = new RecordMesg();
        record.setTimestamp(timestamp);
        record.setHeartRate(heartRate);
        record.setDistance(distance);
        return record;
    }
}
