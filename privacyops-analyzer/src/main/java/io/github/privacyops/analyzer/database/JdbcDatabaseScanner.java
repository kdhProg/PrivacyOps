package io.github.privacyops.analyzer.database;

import io.github.privacyops.fact.DatabaseColumnFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.scan.DatabaseScanner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;

public class JdbcDatabaseScanner
        implements DatabaseScanner {

    @Override
    public List<Fact> scan(
            Connection connection,
            String schema
    ) {

        if (connection == null) {
            throw new IllegalArgumentException(
                    "Database connection must not be null."
            );
        }

        if (schema == null
                || schema.isBlank()) {

            throw new IllegalArgumentException(
                    "Database schema must not be blank."
            );
        }

        List<Fact> facts =
                new ArrayList<>();

        try {

            DatabaseMetaData metadata =
                    connection.getMetaData();

            try (ResultSet columns =
                         metadata.getColumns(
                                 null,
                                 schema,
                                 "%",
                                 "%"
                         )) {

                while (columns.next()) {

                    String schemaName =
                            columns.getString(
                                    "TABLE_SCHEM"
                            );

                    String tableName =
                            columns.getString(
                                    "TABLE_NAME"
                            );

                    String columnName =
                            columns.getString(
                                    "COLUMN_NAME"
                            );

                    String columnType =
                            columns.getString(
                                    "TYPE_NAME"
                            );

                    String comment =
                            columns.getString(
                                    "REMARKS"
                            );

                    String id =
                            createId(
                                    schemaName,
                                    tableName,
                                    columnName
                            );

                    DatabaseColumnFact fact =
                            new DatabaseColumnFact(
                                    id,
                                    schemaName,
                                    tableName,
                                    columnName,
                                    columnType,
                                    comment,
                                    null
                            );

                    facts.add(fact);
                }
            }

        } catch (SQLException e) {

            throw new IllegalStateException(
                    "Failed to scan database metadata "
                            + "for schema: "
                            + schema,
                    e
            );
        }

        return facts;
    }

    private String createId(
            String schema,
            String table,
            String column
    ) {

        return "db-column:"
                + schema
                + "."
                + table
                + "."
                + column;
    }
}