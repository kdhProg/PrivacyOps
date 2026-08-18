package io.github.privacyops.report.html;

import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.policy.PrivacyPolicy;

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
    }
}