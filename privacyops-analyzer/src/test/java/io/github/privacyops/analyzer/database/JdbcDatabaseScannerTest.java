package io.github.privacyops.analyzer.database;

import io.github.privacyops.analyzer.classifier.NamePatternPrivacyClassifier;
import io.github.privacyops.fact.DatabaseColumnFact;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.model.PrivacyType;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcDatabaseScannerTest {

    @Test
    void scansDatabaseColumns()
            throws Exception {

        try (Connection connection =
                     DriverManager.getConnection(
                             "jdbc:h2:mem:privacyops;"
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

            /*
             * 1. 실제 JDBC Metadata Scanner 실행
             */
            JdbcDatabaseScanner scanner =
                    new JdbcDatabaseScanner();

            List<Fact> facts =
                    scanner.scan(
                            connection,
                            "APP"
                    );

            /*
             * 2. DatabaseColumnFact만 추출
             */
            List<DatabaseColumnFact> columns =
                    facts.stream()
                            .filter(
                                    DatabaseColumnFact.class
                                            ::isInstance
                            )
                            .map(
                                    DatabaseColumnFact.class
                                            ::cast
                            )
                            .toList();

            /*
             * 3. DB Metadata 자체가 정상적으로
             *    추출되었는지 검증
             */
            assertTrue(
                    columns.stream()
                            .anyMatch(
                                    column ->
                                            column.tableName()
                                                    .equals(
                                                            "TB_MEMBER"
                                                    )
                                                    && column.columnName()
                                                    .equals(
                                                            "RRN"
                                                    )
                            )
            );

            assertTrue(
                    columns.stream()
                            .anyMatch(
                                    column ->
                                            column.columnName()
                                                    .equals(
                                                            "EMAIL_ADDR"
                                                    )
                            )
            );

            assertTrue(
                    columns.stream()
                            .anyMatch(
                                    column ->
                                            column.columnName()
                                                    .equals(
                                                            "PHONE_NO"
                                                    )
                            )
            );

            /*
             * 4. 기존 Privacy Classifier 연결
             *
             * DatabaseColumnFact의 컬럼명을 보고
             * 개인정보 유형을 분류할 수 있는지 검증
             */
            NamePatternPrivacyClassifier classifier =
                    new NamePatternPrivacyClassifier();

            /*
             * RRN 컬럼이 NATIONAL_IDENTIFIER로
             * 분류되는지 확인
             */
            boolean rrnClassified =
                    facts.stream()
                            .map(
                                    classifier::classify
                            )
                            .flatMap(
                                    Optional::stream
                            )
                            .anyMatch(
                                    classification ->
                                            classification
                                                    .privacyType()
                                                    == PrivacyType
                                                    .NATIONAL_IDENTIFIER
                            );

            assertTrue(
                    rrnClassified
            );

            /*
             * EMAIL_ADDR 컬럼이 EMAIL로
             * 분류되는지 확인
             */
            boolean emailClassified =
                    facts.stream()
                            .map(
                                    classifier::classify
                            )
                            .flatMap(
                                    Optional::stream
                            )
                            .anyMatch(
                                    classification ->
                                            classification
                                                    .privacyType()
                                                    == PrivacyType.EMAIL
                            );

            assertTrue(
                    emailClassified
            );

            /*
             * PHONE_NO 컬럼이 PHONE_NUMBER로
             * 분류되는지 확인
             */
            boolean phoneClassified =
                    facts.stream()
                            .map(
                                    classifier::classify
                            )
                            .flatMap(
                                    Optional::stream
                            )
                            .anyMatch(
                                    classification ->
                                            classification
                                                    .privacyType()
                                                    == PrivacyType
                                                    .PHONE_NUMBER
                            );

            assertTrue(
                    phoneClassified
            );
        }
    }
}