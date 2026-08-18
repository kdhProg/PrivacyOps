package io.github.privacyops.analyzer.engine;

import io.github.privacyops.engine.PrivacyOpsEngine;
import io.github.privacyops.fact.DatabaseColumnFact;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.policy.PrivacyPolicy;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseIntegratedPrivacyOpsEngineTest {

    @Test
    void analyzesDatabaseToApiFlow()
            throws Exception {

        Path projectRoot =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample"
                                )
                                .toURI()
                );

        try (Connection connection =
                     DriverManager.getConnection(
                             "jdbc:h2:mem:privacyops_engine;"
                                     + "DB_CLOSE_DELAY=-1"
                     )) {

            try (Statement statement =
                         connection.createStatement()) {

                statement.execute(
                        "CREATE SCHEMA IF NOT EXISTS APP"
                );

                statement.execute(
                        """
                        CREATE TABLE APP.TB_MEMBER (
                            MEMBER_ID BIGINT,
                            NAME VARCHAR(100),
                            RRN VARCHAR(20),
                            EMAIL_ADDR VARCHAR(200),
                            PHONE_NO VARCHAR(30)
                        )
                        """
                );
            }

            PrivacyOpsEngine engine =
                    PrivacyOpsEngineFactory
                            .createDefault();

            AnalysisResult result =
                    engine.analyze(
                            projectRoot,
                            PrivacyPolicy.empty(),
                            connection,
                            "APP"
                    );

            boolean databaseMapperFlowExists =
                    result.dataFlows()
                            .stream()
                            .anyMatch(
                                    edge ->
                                            edge.relation()
                                                    == DataFlowRelation
                                                    .DATABASE_MAPPER
                            );

            assertTrue(
                    databaseMapperFlowExists
            );

            boolean mapperResultFlowExists =
                    result.dataFlows()
                            .stream()
                            .anyMatch(
                                    edge ->
                                            edge.relation()
                                                    == DataFlowRelation
                                                    .MAPPER_RESULT
                            );

            assertTrue(
                    mapperResultFlowExists
            );

            boolean apiFlowExists =
                    result.dataFlows()
                            .stream()
                            .anyMatch(
                                    edge ->
                                            edge.relation()
                                                    == DataFlowRelation
                                                    .API_RESPONSE
                            );

            assertTrue(
                    apiFlowExists
            );

            boolean databaseRrnClassified =
                    result.classifiedFacts()
                            .stream()
                            .anyMatch(
                                    classified ->
                                            classified.fact()
                                                    instanceof DatabaseColumnFact column
                                                    && column.columnName()
                                                    .equals("RRN")
                                                    && classified
                                                    .classification()
                                                    .privacyType()
                                                    == PrivacyType
                                                    .NATIONAL_IDENTIFIER
                            );

            assertTrue(
                    databaseRrnClassified
            );
        }

    }
}