package io.github.privacyops.cli;

import io.github.privacyops.analyzer.PrivacyAnalysisService;
import io.github.privacyops.analyzer.classifier.NamePatternPrivacyClassifier;
import io.github.privacyops.analyzer.flow.MapperResultFlowLinker;
import io.github.privacyops.analyzer.java.JavaSourceScanner;
import io.github.privacyops.analyzer.mybatis.MyBatisMapperScanner;
import io.github.privacyops.analyzer.policy.YamlPolicyProvider;
import io.github.privacyops.analyzer.project.DefaultProjectScanner;

import io.github.privacyops.analyzer.rule.MissingResourcePolicyRule;
import io.github.privacyops.analyzer.spring.SpringControllerScanner;
import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.fact.MapperQueryFact;
import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.policy.PrivacyPolicy;
import io.github.privacyops.scan.ScanResult;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import io.github.privacyops.analyzer.rule.ApiPrivacyExposureRule;

import io.github.privacyops.model.Finding;

import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;

import io.github.privacyops.analyzer.flow.ControllerResponseFlowLinker;
import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.JavaFieldFact;
import io.github.privacyops.flow.DataFlowEdge;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.ArrayList;

import static picocli.CommandLine.*;


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

        // 3. Scanner 구성
        DefaultProjectScanner projectScanner =
                new DefaultProjectScanner(
                        List.of(
                                new JavaSourceScanner(),
                                new SpringControllerScanner(),
                                new MyBatisMapperScanner()
                        )
                );

        // 4. 분석 서비스 구성
        PrivacyAnalysisService analysisService =
                new PrivacyAnalysisService(
                        projectScanner,
                        List.of(
                                new NamePatternPrivacyClassifier()
                        )
                );

        // 5. 프로젝트 Scan
        ScanResult scanResult =
                analysisService.scan(projectPath);

        List<ClassifiedFact> classifiedFacts =
                analysisService.classify(
                        scanResult.facts()
                );

        // 6. 데이터 흐름 연결
        List<DataFlowEdge> edges =
                new ArrayList<>();

        MapperResultFlowLinker mapperFlowLinker =
                new MapperResultFlowLinker();

        edges.addAll(
                mapperFlowLinker.link(
                        scanResult.facts()
                )
        );

        ControllerResponseFlowLinker apiFlowLinker =
                new ControllerResponseFlowLinker();

        edges.addAll(
                apiFlowLinker.link(
                        scanResult.facts()
                )
        );

        // 7. Rule 평가용 Context 생성
        RuleContext ruleContext =
                new RuleContext(
                        scanResult.facts(),
                        classifiedFacts,
                        edges,
                        privacyPolicy
                );

        // 8. 실행 Rule 등록
        List<PrivacyRule> rules =
                List.of(
                        new ApiPrivacyExposureRule(),
                        new MissingResourcePolicyRule()
                );

        // 9. Rule 실행
        List<Finding> findings =
                rules.stream()
                        .flatMap(
                                rule ->
                                        rule.evaluate(ruleContext)
                                                .stream()
                        )
                        .toList();

        // 10. 결과 출력
        printSummary(
                projectPath,
                scanResult,
                classifiedFacts
        );

        printPolicyStatus(
                privacyPolicy
        );

        printMapperQueries(
                scanResult.facts()
        );

        printMapperFlows(
                scanResult.facts(),
                classifiedFacts,
                edges
        );

        printApiFlows(
                scanResult.facts(),
                classifiedFacts,
                edges
        );

        printFindings(
                findings
        );

        return 0;
    }

    private void printSummary(
            Path projectPath,
            ScanResult scanResult,
            List<ClassifiedFact> classifiedFacts
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
                scanResult.facts().size()
        );

        System.out.printf(
                "%-22s : %d%n",
                "Privacy Candidates",
                classifiedFacts.size()
        );

        System.out.printf(
                "%-22s : %d%n",
                "Warnings",
                scanResult.warnings().size()
        );

        printPrivacyTypes(classifiedFacts);

        printWarnings(scanResult);
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
            ScanResult scanResult
    ) {

        if (scanResult.warnings().isEmpty()) {
            return;
        }

        System.out.println();
        System.out.println("Warnings");
        System.out.println(
                "--------------------------------"
        );

        scanResult.warnings()
                .forEach(
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

                System.out.printf(
                        "Location: %s:%s%n",
                        finding.location().file(),
                        finding.location().line()
                );
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
                                            + resource.retention()
                            );

                            System.out.println(
                                    "  disposal  : "
                                            + resource.disposal()
                            );
                        }
                );
    }

}