package io.github.privacyops.report.html;

import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.policy.PrivacyPolicy;
import io.github.privacyops.risk.RiskAssessment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HtmlReportWriterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void writesHtmlReport()
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

        Path output =
                tempDirectory.resolve(
                        "privacyops-report.html"
                );

        HtmlReportWriter writer =
                new HtmlReportWriter();

        /*
         * 기존 호환 경로:
         * Risk 정보 없이도 HTML Report 생성 가능
         */
        writer.write(
                result,
                output
        );

        assertTrue(
                Files.exists(output)
        );

        String html =
                Files.readString(output);

        assertTrue(
                html.contains(
                        "PrivacyOps Analysis Report"
                )
        );

        assertTrue(
                html.contains(
                        "Privacy Candidates"
                )
        );

        assertTrue(
                html.contains(
                        "Governance Coverage"
                )
        );
    }

    @Test
    void writesRiskAssessmentToHtmlReport()
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

        List<RiskAssessment> riskAssessments =
                List.of(
                        new RiskAssessment(
                                "java-field:test#residentNumber",
                                PrivacyType.NATIONAL_IDENTIFIER,
                                10,
                                "CRITICAL",
                                "Custom privacy type weight"
                        ),
                        new RiskAssessment(
                                "java-field:test#emailAddress",
                                PrivacyType.EMAIL,
                                3,
                                "MEDIUM",
                                "Custom privacy type weight"
                        )
                );

        Path output =
                tempDirectory.resolve(
                        "privacyops-risk-report.html"
                );

        HtmlReportWriter writer =
                new HtmlReportWriter();

        /*
         * Risk 포함 overload 사용
         */
        writer.write(
                result,
                riskAssessments,
                output
        );

        assertTrue(
                Files.exists(output)
        );

        String html =
                Files.readString(output);

        assertTrue(
                html.contains(
                        "Privacy Risk Profile"
                )
        );

        assertTrue(
                html.contains(
                        "NATIONAL_IDENTIFIER"
                )
        );

        assertTrue(
                html.contains(
                        "CRITICAL"
                )
        );

        assertTrue(
                html.contains(
                        "10"
                )
        );

        assertTrue(
                html.contains(
                        "EMAIL"
                )
        );

        assertTrue(
                html.contains(
                        "MEDIUM"
                )
        );
    }
}