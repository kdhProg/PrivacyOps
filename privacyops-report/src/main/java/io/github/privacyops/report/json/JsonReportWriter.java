package io.github.privacyops.report.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.report.PrivacyReportWriter;
import io.github.privacyops.report.ReportContext;
import io.github.privacyops.report.score.GovernanceScore;
import io.github.privacyops.report.score.GovernanceScoreCalculator;
import io.github.privacyops.risk.RiskAssessment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonReportWriter
        implements PrivacyReportWriter {

    private final ObjectMapper objectMapper;

    public JsonReportWriter() {

        this.objectMapper =
                new ObjectMapper();

        this.objectMapper.enable(
                SerializationFeature.INDENT_OUTPUT
        );
    }

    @Override
    public String format() {

        return "json";
    }

    @Override
    public void write(
            ReportContext context,
            Path outputPath
    ) {

        if (context == null) {

            throw new IllegalArgumentException(
                    "Report context must not be null."
            );
        }

        if (outputPath == null) {

            throw new IllegalArgumentException(
                    "Output path must not be null."
            );
        }

        AnalysisResult result =
                context.analysisResult();

        List<RiskAssessment> risks =
                context.riskAssessments();

        GovernanceScore governance =
                new GovernanceScoreCalculator()
                        .calculate(result);

        Map<String, Object> report =
                new LinkedHashMap<>();

        report.put(
                "generator",
                "PrivacyOps"
        );

        report.put(
                "version",
                "0.1.0"
        );

        report.put(
                "summary",
                createSummary(
                        result,
                        risks,
                        governance
                )
        );

        report.put(
                "privacyTypes",
                createPrivacyTypes(
                        result
                )
        );

        report.put(
                "governance",
                createGovernance(
                        governance
                )
        );

        report.put(
                "riskAssessments",
                risks
        );

        report.put(
                "dataFlows",
                result.dataFlows()
        );

        report.put(
                "findings",
                result.findings()
        );

        report.put(
                "policy",
                result.policy()
        );

        try {

            Path parent =
                    outputPath
                            .toAbsolutePath()
                            .getParent();

            if (parent != null) {

                Files.createDirectories(
                        parent
                );
            }

            objectMapper.writeValue(
                    outputPath.toFile(),
                    report
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to write JSON report: "
                            + outputPath,
                    e
            );
        }
    }

    private Map<String, Object> createSummary(
            AnalysisResult result,
            List<RiskAssessment> risks,
            GovernanceScore governance
    ) {

        Map<String, Object> summary =
                new LinkedHashMap<>();

        summary.put(
                "facts",
                result.facts().size()
        );

        summary.put(
                "privacyCandidates",
                result.classifiedFacts().size()
        );

        summary.put(
                "dataFlows",
                result.dataFlows().size()
        );

        summary.put(
                "findings",
                result.findings().size()
        );

        long criticalFindings =
                result.findings()
                        .stream()
                        .filter(
                                finding ->
                                        finding.severity()
                                                .name()
                                                .equals(
                                                        "CRITICAL"
                                                )
                        )
                        .count();

        summary.put(
                "criticalFindings",
                criticalFindings
        );

        summary.put(
                "governanceCoverage",
                governance.score()
        );

        String highestRisk =
                risks.stream()
                        .max(
                                Comparator.comparingInt(
                                        RiskAssessment::weight
                                )
                        )
                        .map(
                                RiskAssessment::level
                        )
                        .orElse(
                                "N/A"
                        );

        summary.put(
                "highestPrivacyRisk",
                highestRisk
        );

        return summary;
    }

    private Map<PrivacyType, Long>
    createPrivacyTypes(
            AnalysisResult result
    ) {

        Map<PrivacyType, Long> counts =
                new EnumMap<>(
                        PrivacyType.class
                );

        for (ClassifiedFact classified :
                result.classifiedFacts()) {

            PrivacyType type =
                    classified
                            .classification()
                            .privacyType();

            counts.merge(
                    type,
                    1L,
                    Long::sum
            );
        }

        return counts;
    }

    private Map<String, Object> createGovernance(
            GovernanceScore governance
    ) {

        Map<String, Object> controls =
                new LinkedHashMap<>();

        controls.put(
                "resourcePolicy",
                governance.resourcePolicy()
        );

        controls.put(
                "retention",
                governance.retention()
        );

        controls.put(
                "disposal",
                governance.disposal()
        );

        controls.put(
                "accessControl",
                governance.accessControl()
        );

        controls.put(
                "auditControl",
                governance.auditControl()
        );

        return controls;
    }
}