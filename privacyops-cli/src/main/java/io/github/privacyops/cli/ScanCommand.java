package io.github.privacyops.cli;

import io.github.privacyops.analyzer.engine.PrivacyOpsEngineFactory;
import io.github.privacyops.analyzer.policy.YamlPolicyProvider;
import io.github.privacyops.engine.PrivacyOpsEngine;
import io.github.privacyops.fact.*;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.policy.PrivacyPolicy;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static picocli.CommandLine.Option;


@Command(
        name = "scan",
        description = "Scan a project for privacy-related data.",
        mixinStandardHelpOptions = true
)
public class ScanCommand implements Callable<Integer> {

    @Option(
            names = {
                    "-p",
                    "--policy"
            },
            paramLabel = "<policy-file>",
            description =
                    "Privacy policy YAML file."
    )
    private Path policyPath;


    @Parameters(
            index = "0",
            paramLabel = "<project-path>",
            description = "Project directory to scan."
    )
    private Path projectPath;

    @Option(
            names = "--db-url",
            paramLabel = "<jdbc-url>",
            description =
                    "JDBC URL for database metadata scanning."
    )
    private String databaseUrl;

    @Option(
            names = "--db-user",
            paramLabel = "<username>",
            description =
                    "Database username."
    )
    private String databaseUser;

    @Option(
            names = "--db-schema",
            paramLabel = "<schema>",
            description =
                    "Database schema to scan."
    )
    private String databaseSchema;

    @Option(
            names = "--db-password-env",
            paramLabel = "<env-name>",
            description =
                    "Environment variable containing "
                            + "the database password."
    )
    private String databasePasswordEnvironment;

    @Override
    public Integer call() {

        // 1. 프로젝트 경로 검증
        if (!Files.isDirectory(projectPath)) {

            System.err.println(
                    "Project path does not exist or "
                            + "is not a directory: "
                            + projectPath
            );

            return 2;
        }

        // 2. Privacy Policy 로딩
        PrivacyPolicy privacyPolicy =
                PrivacyPolicy.empty();

        if (policyPath != null) {

            if (!Files.isRegularFile(policyPath)) {

                System.err.println(
                        "Policy file does not exist: "
                                + policyPath
                );

                return 2;
            }

            YamlPolicyProvider policyProvider =
                    new YamlPolicyProvider();

            privacyPolicy =
                    policyProvider.load(
                            policyPath
                    );
        }

        // 3. DB 옵션 검증
        if (hasAnyDatabaseOption()
                && !hasAllDatabaseOptions()) {

            System.err.println(
                    "Database scan requires all of: "
                            + "--db-url, "
                            + "--db-user, "
                            + "--db-schema, "
                            + "--db-password-env"
            );

            return 2;
        }

        // 4. Engine 생성
        PrivacyOpsEngine engine =
                PrivacyOpsEngineFactory
                        .createDefault();

        AnalysisResult result;

        // 5-A. DB 포함 분석
        if (hasAllDatabaseOptions()) {

            String databasePassword =
                    resolveDatabasePassword();

            if (databasePassword == null) {

                System.err.println(
                        "Database password environment "
                                + "variable is not set: "
                                + databasePasswordEnvironment
                );

                return 2;
            }

            try (Connection connection =
                         DriverManager.getConnection(
                                 databaseUrl,
                                 databaseUser,
                                 databasePassword
                         )) {

                result =
                        engine.analyze(
                                projectPath,
                                privacyPolicy,
                                connection,
                                databaseSchema
                        );

            } catch (SQLException e) {

                System.err.println(
                        "Failed to connect to database: "
                                + e.getMessage()
                );

                return 3;
            }

        } else {

            // 5-B. 기존 Source-only 분석
            result =
                    engine.analyze(
                            projectPath,
                            privacyPolicy
                    );
        }

        // 6. 결과 출력
        printSummary(
                projectPath,
                result
        );

        printDatabaseStatus(
                result
        );

        printPolicyStatus(
                result.policy()
        );

        printMapperQueries(
                result.facts()
        );

        printDatabaseFlows(
                result
        );

        printMapperFlows(
                result.facts(),
                result.classifiedFacts(),
                result.dataFlows()
        );

        printApiFlows(
                result.facts(),
                result.classifiedFacts(),
                result.dataFlows()
        );

        printFindings(
                result.findings()
        );

        return 0;
    }

    private void printSummary(
            Path projectPath,
            AnalysisResult result
    ) {

        System.out.println();
        System.out.println("PrivacyOps 0.1.0");

        System.out.println();
        System.out.println(
                "Project: "
                        + projectPath.toAbsolutePath()
        );

        System.out.println();
        System.out.println("Scan Summary");
        System.out.println(
                "--------------------------------"
        );

        System.out.printf(
                "%-22s : %d%n",
                "Facts",
                result.facts().size()
        );

        System.out.printf(
                "%-22s : %d%n",
                "Privacy Candidates",
                result.classifiedFacts().size()
        );

        System.out.printf(
                "%-22s : %d%n",
                "Warnings",
                result.warnings().size()
        );

        printPrivacyTypes(
                result.classifiedFacts()
        );

        printWarnings(
                result.warnings()
        );
    }

    private void printPrivacyTypes(
            List<ClassifiedFact> classifiedFacts
    ) {

        Map<PrivacyType, Long> counts =
                new EnumMap<>(PrivacyType.class);

        for (ClassifiedFact classified : classifiedFacts) {

            PrivacyType type =
                    classified
                            .classification()
                            .privacyType();

            counts.merge(type, 1L, Long::sum);
        }

        System.out.println();
        System.out.println("Privacy Types");
        System.out.println(
                "--------------------------------"
        );

        counts.forEach(
                (type, count) ->
                        System.out.printf(
                                "%-22s : %d%n",
                                type,
                                count
                        )
        );
    }

    private void printWarnings(
            List<String> warnings
    ) {

        if (warnings.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("Warnings");
        System.out.println(
                "--------------------------------"
        );

        warnings.forEach(
                warning ->
                        System.out.println(
                                "- " + warning
                        )
        );
    }


    private void printApiFlows(
            List<io.github.privacyops.fact.Fact> facts,
            List<ClassifiedFact> classifiedFacts,
            List<DataFlowEdge> edges
    ) {

        System.out.println();
        System.out.println("API Privacy Flows");
        System.out.println(
                "--------------------------------"
        );

        for (DataFlowEdge edge : edges) {

            ApiEndpointFact endpoint =
                    facts.stream()
                            .filter(
                                    fact ->
                                            fact.id()
                                                    .equals(
                                                            edge.targetFactId()
                                                    )
                            )
                            .filter(
                                    ApiEndpointFact.class
                                            ::isInstance
                            )
                            .map(
                                    ApiEndpointFact.class
                                            ::cast
                            )
                            .findFirst()
                            .orElse(null);

            JavaFieldFact field =
                    facts.stream()
                            .filter(
                                    fact ->
                                            fact.id()
                                                    .equals(
                                                            edge.sourceFactId()
                                                    )
                            )
                            .filter(
                                    JavaFieldFact.class
                                            ::isInstance
                            )
                            .map(
                                    JavaFieldFact.class
                                            ::cast
                            )
                            .findFirst()
                            .orElse(null);

            if (endpoint == null
                    || field == null) {
                continue;
            }

            classifiedFacts.stream()
                    .filter(
                            classified ->
                                    classified.fact()
                                            .id()
                                            .equals(
                                                    field.id()
                                            )
                    )
                    .findFirst()
                    .ifPresent(
                            classified -> {

                                System.out.printf(
                                        "%s %s%n",
                                        endpoint.httpMethod(),
                                        endpoint.path()
                                );

                                System.out.printf(
                                        "  %-18s -> %s%n",
                                        field.fieldName(),
                                        classified
                                                .classification()
                                                .privacyType()
                                );
                            }
                    );
        }
    }

    private void printFindings(
            List<Finding> findings
    ) {

        System.out.println();
        System.out.println("Findings");
        System.out.println(
                "--------------------------------"
        );

        if (findings.isEmpty()) {

            System.out.println(
                    "No privacy findings."
            );

            return;
        }

        for (Finding finding : findings) {

            System.out.printf(
                    "[%s] %s%n",
                    finding.severity(),
                    finding.ruleId()
            );

            System.out.println(
                    finding.title()
            );

            System.out.println(
                    finding.description()
            );

            if (finding.location() != null) {

                if (finding.location().line() != null) {

                    System.out.printf(
                            "Location: %s:%d%n",
                            finding.location().file(),
                            finding.location().line()
                    );

                } else {

                    System.out.printf(
                            "Location: %s%n",
                            finding.location().file()
                    );
                }
            }

            System.out.println();
        }
    }

    private void printMapperQueries(
            List<io.github.privacyops.fact.Fact> facts
    ) {

        List<MapperQueryFact> queries =
                facts.stream()
                        .filter(
                                MapperQueryFact.class
                                        ::isInstance
                        )
                        .map(
                                MapperQueryFact.class
                                        ::cast
                        )
                        .toList();

        if (queries.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("MyBatis Queries");
        System.out.println(
                "--------------------------------"
        );

        for (MapperQueryFact query :
                queries) {

            System.out.println(
                    query.mapperId()
            );

            System.out.println(
                    "  resultType : "
                            + query.resultType()
            );

            System.out.println(
                    "  tables     : "
                            + query.tables()
            );

            System.out.println(
                    "  columns    : "
                            + query.columns()
            );
        }
    }

    private void printMapperFlows(
            List<io.github.privacyops.fact.Fact> facts,
            List<ClassifiedFact> classifiedFacts,
            List<DataFlowEdge> edges
    ) {

        System.out.println();
        System.out.println("Mapper Privacy Flows");
        System.out.println(
                "--------------------------------"
        );

        for (DataFlowEdge edge : edges) {

            if (edge.relation()
                    != DataFlowRelation.MAPPER_RESULT) {

                continue;
            }

            MapperColumnFact column =
                    facts.stream()
                            .filter(
                                    fact ->
                                            fact.id()
                                                    .equals(
                                                            edge.sourceFactId()
                                                    )
                            )
                            .filter(
                                    MapperColumnFact.class
                                            ::isInstance
                            )
                            .map(
                                    MapperColumnFact.class
                                            ::cast
                            )
                            .findFirst()
                            .orElse(null);

            JavaFieldFact field =
                    facts.stream()
                            .filter(
                                    fact ->
                                            fact.id()
                                                    .equals(
                                                            edge.targetFactId()
                                                    )
                            )
                            .filter(
                                    JavaFieldFact.class
                                            ::isInstance
                            )
                            .map(
                                    JavaFieldFact.class
                                            ::cast
                            )
                            .findFirst()
                            .orElse(null);

            if (column == null
                    || field == null) {

                continue;
            }

            ClassifiedFact classified =
                    classifiedFacts.stream()
                            .filter(
                                    item ->
                                            item.fact()
                                                    .id()
                                                    .equals(
                                                            field.id()
                                                    )
                            )
                            .findFirst()
                            .orElse(null);

            // 일반 비개인정보 컬럼은 CLI Privacy Flow에서 생략
            if (classified == null) {
                continue;
            }

            System.out.printf(
                    "%s.%s%n",
                    column.tableName(),
                    column.columnName()
            );

            System.out.printf(
                    "  -> %s.%s [%s]%n",
                    field.className(),
                    field.fieldName(),
                    classified
                            .classification()
                            .privacyType()
            );
        }
    }

    private void printPolicyStatus(
            PrivacyPolicy policy
    ) {

        System.out.println();
        System.out.println("Privacy Policies");
        System.out.println(
                "--------------------------------"
        );

        if (policy.resources().isEmpty()) {

            System.out.println(
                    "No resource policies loaded."
            );

            return;
        }

        policy.resources()
                .forEach(
                        (resourceName, resource) -> {

                            System.out.println(
                                    resourceName
                            );

                            System.out.println(
                                    "  purpose   : "
                                            + resource.purpose()
                            );

                            System.out.println(
                                    "  retention : "
                                            + displayPolicyValue(
                                            resource.retention()
                                    )
                            );

                            System.out.println(
                                    "  disposal  : "
                                            + displayPolicyValue(
                                            resource.disposal()
                                    )
                            );
                        }
                );
    }

    private String displayPolicyValue(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "NOT SET";
        }

        return value;
    }

    private boolean hasAnyDatabaseOption() {

        return !isBlank(databaseUrl)
                || !isBlank(databaseUser)
                || !isBlank(databaseSchema)
                || !isBlank(
                databasePasswordEnvironment
        );
    }

    private boolean hasAllDatabaseOptions() {

        return !isBlank(databaseUrl)
                && !isBlank(databaseUser)
                && !isBlank(databaseSchema)
                && !isBlank(
                databasePasswordEnvironment
        );
    }

    private boolean isBlank(
            String value
    ) {

        return value == null
                || value.isBlank();
    }

//    private String resolveDatabasePassword() {
//
//        if (isBlank(
//                databasePasswordEnvironment
//        )) {
//
//            return null;
//        }
//
//        return System.getenv(
//                databasePasswordEnvironment
//        );
//    }

    protected String resolveDatabasePassword() {

        if (isBlank(
                databasePasswordEnvironment
        )) {
            return null;
        }

        return System.getenv(
                databasePasswordEnvironment
        );
    }

    private void printDatabaseStatus(
            AnalysisResult result
    ) {

        List<DatabaseColumnFact> columns =
                result.facts()
                        .stream()
                        .filter(
                                DatabaseColumnFact.class
                                        ::isInstance
                        )
                        .map(
                                DatabaseColumnFact.class
                                        ::cast
                        )
                        .toList();

        if (columns.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("Database Metadata");
        System.out.println(
                "--------------------------------"
        );

        System.out.println(
                "Columns scanned : "
                        + columns.size()
        );

        columns.stream()
                .map(
                        column ->
                                column.schemaName()
                                        + "."
                                        + column.tableName()
                )
                .distinct()
                .forEach(
                        table ->
                                System.out.println(
                                        "  " + table
                                )
                );
    }

    private void printDatabaseFlows(
            AnalysisResult result
    ) {

        List<DataFlowEdge> databaseEdges =
                result.dataFlows()
                        .stream()
                        .filter(
                                edge ->
                                        edge.relation()
                                                == DataFlowRelation
                                                .DATABASE_MAPPER
                        )
                        .toList();

        if (databaseEdges.isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("Database Privacy Flows");
        System.out.println(
                "--------------------------------"
        );

        for (DataFlowEdge edge :
                databaseEdges) {

            DatabaseColumnFact databaseColumn =
                    result.facts()
                            .stream()
                            .filter(
                                    fact ->
                                            fact.id()
                                                    .equals(
                                                            edge.sourceFactId()
                                                    )
                            )
                            .filter(
                                    DatabaseColumnFact.class
                                            ::isInstance
                            )
                            .map(
                                    DatabaseColumnFact.class
                                            ::cast
                            )
                            .findFirst()
                            .orElse(null);

            MapperColumnFact mapperColumn =
                    result.facts()
                            .stream()
                            .filter(
                                    fact ->
                                            fact.id()
                                                    .equals(
                                                            edge.targetFactId()
                                                    )
                            )
                            .filter(
                                    MapperColumnFact.class
                                            ::isInstance
                            )
                            .map(
                                    MapperColumnFact.class
                                            ::cast
                            )
                            .findFirst()
                            .orElse(null);

            if (databaseColumn == null
                    || mapperColumn == null) {
                continue;
            }

            ClassifiedFact classification =
                    result.classifiedFacts()
                            .stream()
                            .filter(
                                    classified ->
                                            classified.fact()
                                                    .id()
                                                    .equals(
                                                            databaseColumn.id()
                                                    )
                            )
                            .findFirst()
                            .orElse(null);

            /*
             * CLI의 Privacy Flow 화면에서는
             * 개인정보로 분류되지 않은 DB 컬럼은 생략
             */
            if (classification == null) {
                continue;
            }

            System.out.printf(
                    "%s.%s.%s [%s]%n",
                    databaseColumn.schemaName(),
                    databaseColumn.tableName(),
                    databaseColumn.columnName(),
                    classification
                            .classification()
                            .privacyType()
            );

            System.out.printf(
                    "  -> %s.%s%n",
                    mapperColumn.tableName(),
                    mapperColumn.columnName()
            );
        }
    }

}