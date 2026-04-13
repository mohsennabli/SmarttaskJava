package org.esprit.gestionprojet.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class MigrationService {
    private static final String MIGRATION_PATH = "db/migration";

    private MigrationService() {
    }

    public static void runMigrations(Connection connection) {
        try {
            createMigrationsTable(connection);
            List<String> migrationFiles = listMigrationFiles();

            for (String fileName : migrationFiles) {
                if (alreadyApplied(connection, fileName)) {
                    continue;
                }

                String sql = readMigrationSql(fileName);
                applyMigration(connection, fileName, sql);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run migrations", e);
        }
    }

    private static void createMigrationsTable(Connection connection) throws Exception {
        String sql = """
                CREATE TABLE IF NOT EXISTS schema_migrations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    migration_name TEXT NOT NULL UNIQUE,
                    applied_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """;

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static List<String> listMigrationFiles() {
        String[] migrationCandidates = {
                "V1__create_user_table.sql",
                "V2__seed_test_users.sql"
        };

        List<String> files = new ArrayList<>();
        for (String candidate : migrationCandidates) {
            String fullPath = MIGRATION_PATH + "/" + candidate;
            InputStream stream = MigrationService.class.getClassLoader().getResourceAsStream(fullPath);
            if (stream != null) {
                files.add(candidate);
                try {
                    stream.close();
                } catch (Exception ignored) {
                }
            }
        }

        return files.stream().sorted(Comparator.naturalOrder()).collect(Collectors.toList());
    }

    private static boolean alreadyApplied(Connection connection, String migrationName) throws Exception {
        String sql = "SELECT COUNT(*) FROM schema_migrations WHERE migration_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, migrationName);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    private static String readMigrationSql(String fileName) throws Exception {
        String resourcePath = MIGRATION_PATH + "/" + fileName;
        InputStream stream = MigrationService.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Missing migration file: " + fileName);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private static void applyMigration(Connection connection, String migrationName, String sql) throws Exception {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);

        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
            try (PreparedStatement insert = connection
                    .prepareStatement("INSERT INTO schema_migrations (migration_name) VALUES (?)")) {
                insert.setString(1, migrationName);
                insert.executeUpdate();
            }
            connection.commit();
        } catch (Exception e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }
}
