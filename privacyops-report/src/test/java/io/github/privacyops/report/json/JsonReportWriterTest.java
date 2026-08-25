package io.github.privacyops.report.json;

import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.policy.PrivacyPolicy;
import io.github.privacyops.report.ReportContext;
import io.github.privacyops.risk.RiskAssessment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonReportWriterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void writesJsonReport()
            throws Exception {

        AnalysisResult result =
                new AnalysisResult(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(),
                        PrivacyPolicy.empty()
                );

        List<RiskAssessment> risks =
                List.of(
                        new RiskAssessment(
                                "test-fact",
                                PrivacyType
                                        .NATIONAL_IDENTIFIER,
                                10,
                                "CRITICAL",
                                "test"
                        )
                );

        ReportContext context =
                new ReportContext(
                        result,
                        risks
                );

        Path output =
                tempDirectory.resolve(
                        "privacyops-report.json"
                );

        JsonReportWriter writer =
                new JsonReportWriter();

        writer.write(
                context,
                output
        );

        assertTrue(
                Files.exists(output)
        );

        String json =
                Files.readString(
                        output
                );

        assertTrue(
                json.contains(
                        "\"generator\" : \"PrivacyOps\""
                )
        );

        assertTrue(
                json.contains(
                        "\"highestPrivacyRisk\" : \"CRITICAL\""
                )
        );

        assertTrue(
                json.contains(
                        "\"governanceCoverage\""
                )
        );

        assertTrue(
                json.contains(
                        "\"riskAssessments\""
                )
        );
    }
}