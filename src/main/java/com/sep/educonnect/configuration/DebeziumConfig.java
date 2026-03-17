package com.sep.educonnect.configuration;

import com.sep.educonnect.repository.*;
import com.sep.educonnect.utils.DebeziumSourceEventListener;
import com.sep.educonnect.utils.StudentSearchProjectionUtil;
import com.sep.educonnect.utils.TutorSearchProjectionUtil;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DebeziumConfig {
    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${DB_ENV:local}")
    private String dbEnv;

    @Bean
    public io.debezium.config.Configuration mysqlConnector() {
        // Extract host, port, and database from JDBC URL
        // jdbc:mysql://localhost:3306/mydb -> host=localhost, port=3306, database=mydb
        String cleanUrl = datasourceUrl.replace("jdbc:mysql://", "");
        String[] parts = cleanUrl.split("/");
        String[] hostPort = parts[0].split(":");
        String host = hostPort[0];
        String port = hostPort.length > 1 ? hostPort[1].split("\\?")[0] : "3306";
        String database = parts.length > 1 ? parts[1].split("\\?")[0] : "";

        return io.debezium.config.Configuration.create()
                // Engine properties
                .with("name", "mysql-connector")
                .with("connector.class", "io.debezium.connector.mysql.MySqlConnector")
                .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
                .with("offset.storage.file.filename", "data/debezium/offsets-" + dbEnv + ".dat")
                .with("offset.flush.interval.ms", "60000")

                // REQUIRED in Debezium 3.x: topic.prefix (replaces database.server.name)
                .with("topic.prefix", "mysql-server")

                // MySQL connection properties
                .with("database.hostname", host)
                .with("database.port", port)
                .with("database.user", username)
                .with("database.password", password)
                .with("database.include.list", database) // Changed from database.dbname in 3.x
                .with("database.server.id", "85744")

                // Schema history (required for MySQL)
                .with("schema.history.internal", "io.debezium.storage.file.history.FileSchemaHistory")
                .with("schema.history.internal.file.filename", "data/debezium/schemahistory-" + dbEnv + ".dat")

                // Capture settings
                .with("table.include.list", database + "\\..*") // Capture all tables, or specify: "mydb.users,mydb.orders"
                .with("include.schema.changes", "false")

                // Snapshot mode: initial, never, when_needed, schema_only
                .with("snapshot.mode", "when_needed")

                .build();
    }

    @Bean
    public DebeziumSourceEventListener debeziumSourceEventListener(
            io.debezium.config.Configuration mysqlConnector,
            TutorProfileRepository tutorProfileRepository,
            TutorAvailabilityRepository tutorAvailabilityRepository,
            TutorClassRepository tutorClassRepository,
            TutorSearchProjectionUtil projectionService,
            StudentSearchProjectionUtil studentProjectionService,
            UserRepository userRepository,
            OpenSearchClient openSearchClient) {
        return new DebeziumSourceEventListener(
                mysqlConnector,
                tutorProfileRepository,
                tutorAvailabilityRepository,
                tutorClassRepository,
                projectionService,
                studentProjectionService,
                userRepository,
                openSearchClient
        );
    }
}
