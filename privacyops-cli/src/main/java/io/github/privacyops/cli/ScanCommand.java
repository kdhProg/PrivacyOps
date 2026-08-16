package io.github.privacyops.cli;

import io.github.privacyops.analyzer.engine.PrivacyOpsEngineFactory;
import io.github.privacyops.analyzer.policy.YamlPolicyProvider;
import io.github.privacyops.engine.PrivacyOpsEngine;
import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.fact.MapperQueryFact;
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

    @Override
    public Integer call() {

        // 1. 프로젝트 경로 검증
        if (!Files.isDirectory(projectPath)) {

            System.err.println(
                    "Project path does not exist or is not a directory: "
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

        // 3. PrivacyOps Engine 생성
        PrivacyOpsEngine engine =
                PrivacyOpsEngineFactory
                        .createDefault();

        // 4. 전체 분석 실행
        AnalysisResult result =
                engine.analyze(
                        projectPath,
                        privacyPolicy
                );

        // 5. 결과 출력
        printSummary(
                projectPath,
                result
        );

        printPolicyStatus(
                result.policy()
        );

        printMapperQueries(
                result.facts()
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

}