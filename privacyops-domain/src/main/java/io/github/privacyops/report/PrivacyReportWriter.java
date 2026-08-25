package io.github.privacyops.report;

import java.nio.file.Path;

public interface PrivacyReportWriter {

    /**
     * Report format identifier.
     *
     * Examples:
     * html
     * json
     * sarif
     */
    String format();

    /**
     * Write PrivacyOps analysis result.
     */
    void write(
            ReportContext context,
            Path outputPath
    );
}