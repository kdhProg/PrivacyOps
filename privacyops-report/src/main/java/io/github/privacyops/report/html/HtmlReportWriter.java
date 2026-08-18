package io.github.privacyops.report.html;

import io.github.privacyops.flow.DataFlowRelation;
import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.Finding;
import io.github.privacyops.model.PrivacyType;
import io.github.privacyops.model.Severity;
import io.github.privacyops.policy.ResourcePolicy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

public class HtmlReportWriter {

    public void write(
            AnalysisResult result,
            Path outputPath
    ) {

        if (result == null) {
            throw new IllegalArgumentException(
                    "Analysis result must not be null."
            );
        }

        if (outputPath == null) {
            throw new IllegalArgumentException(
                    "Output path must not be null."
            );
        }

        String html =
                createHtml(result);

        try {

            Path parent =
                    outputPath
                            .toAbsolutePath()
                            .getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    outputPath,
                    html,
                    StandardCharsets.UTF_8
            );

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Failed to write HTML report: "
                            + outputPath,
                    e
            );
        }
    }

    private String createHtml(
            AnalysisResult result
    ) {

        StringBuilder html =
                new StringBuilder();

        Map<PrivacyType, Long> privacyCounts =
                countPrivacyTypes(result);

        Map<Severity, Long> severityCounts =
                countSeverities(result);

        html.append(
                """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport"
                          content="width=device-width, initial-scale=1.0">
                    <title>PrivacyOps Analysis Report</title>

                    <style>
                        * {
                            box-sizing: border-box;
                        }

                        body {
                            margin: 0;
                            background: #f5f7fa;
                            color: #1f2937;
                            font-family:
                                -apple-system,
                                BlinkMacSystemFont,
                                "Segoe UI",
                                Arial,
                                sans-serif;
                        }

                        .container {
                            max-width: 1200px;
                            margin: 0 auto;
                            padding: 40px 24px 80px;
                        }

                        .header {
                            margin-bottom: 32px;
                        }

                        .title {
                            font-size: 32px;
                            font-weight: 700;
                            margin: 0;
                        }

                        .subtitle {
                            margin-top: 8px;
                            color: #6b7280;
                        }

                        .cards {
                            display: grid;
                            grid-template-columns:
                                repeat(auto-fit, minmax(180px, 1fr));
                            gap: 16px;
                            margin-bottom: 32px;
                        }

                        .card {
                            background: white;
                            border-radius: 12px;
                            padding: 20px;
                            border: 1px solid #e5e7eb;
                        }

                        .card-label {
                            color: #6b7280;
                            font-size: 13px;
                            margin-bottom: 8px;
                        }

                        .card-value {
                            font-size: 28px;
                            font-weight: 700;
                        }

                        .section {
                            background: white;
                            border: 1px solid #e5e7eb;
                            border-radius: 12px;
                            padding: 24px;
                            margin-bottom: 24px;
                        }

                        .section-title {
                            margin: 0 0 20px;
                            font-size: 20px;
                        }

                        table {
                            width: 100%;
                            border-collapse: collapse;
                        }

                        th,
                        td {
                            text-align: left;
                            padding: 12px;
                            border-bottom: 1px solid #e5e7eb;
                            vertical-align: top;
                        }

                        th {
                            color: #6b7280;
                            font-size: 13px;
                        }

                        .badge {
                            display: inline-block;
                            padding: 4px 9px;
                            border-radius: 6px;
                            background: #eef2f7;
                            font-size: 12px;
                            font-weight: 600;
                        }

                        .finding {
                            padding: 18px 0;
                            border-bottom: 1px solid #e5e7eb;
                        }

                        .finding:last-child {
                            border-bottom: none;
                        }

                        .finding-title {
                            font-weight: 700;
                            margin: 8px 0;
                        }

                        .finding-description {
                            color: #374151;
                            line-height: 1.6;
                        }

                        .location {
                            margin-top: 8px;
                            color: #6b7280;
                            font-size: 12px;
                            word-break: break-all;
                        }

                        .empty {
                            color: #6b7280;
                        }

                        .footer {
                            color: #9ca3af;
                            font-size: 12px;
                            margin-top: 32px;
                        }
                    </style>
                </head>

                <body>
                <div class="container">

                    <div class="header">
                        <h1 class="title">
                            PrivacyOps Analysis Report
                        </h1>

                        <div class="subtitle">
                            Privacy governance analysis
                            across application data flows
                        </div>
                    </div>
                """
        );

        appendSummaryCards(
                html,
                result,
                severityCounts
        );

        appendPrivacyTypes(
                html,
                privacyCounts
        );

        appendDataFlowSummary(
                html,
                result
        );

        appendPolicies(
                html,
                result
        );

        appendFindings(
                html,
                result
        );

        html.append(
                """
                    <div class="footer">
                        Generated by PrivacyOps 0.1.0
                    </div>

                </div>
                </body>
                </html>
                """
        );

        return html.toString();
    }

    private void appendSummaryCards(
            StringBuilder html,
            AnalysisResult result,
            Map<Severity, Long> severityCounts
    ) {

        html.append(
                """
                <div class="cards">
                """
        );

        appendCard(
                html,
                "Facts",
                result.facts().size()
        );

        appendCard(
                html,
                "Privacy Candidates",
                result.classifiedFacts().size()
        );

        appendCard(
                html,
                "Data Flows",
                result.dataFlows().size()
        );

        appendCard(
                html,
                "Findings",
                result.findings().size()
        );

        appendCard(
                html,
                "Critical Findings",
                severityCounts.getOrDefault(
                        Severity.CRITICAL,
                        0L
                )
        );

        html.append(
                """
                </div>
                """
        );
    }

    private void appendCard(
            StringBuilder html,
            String label,
            long value
    ) {

        html.append(
                """
                <div class="card">
                    <div class="card-label">
                """
        );

        html.append(
                escape(label)
        );

        html.append(
                """
                    </div>

                    <div class="card-value">
                """
        );

        html.append(value);

        html.append(
                """
                    </div>
                </div>
                """
        );
    }

    private void appendPrivacyTypes(
            StringBuilder html,
            Map<PrivacyType, Long> counts
    ) {

        html.append(
                """
                <div class="section">
                    <h2 class="section-title">
                        Privacy Types
                    </h2>

                    <table>
                        <thead>
                        <tr>
                            <th>Type</th>
                            <th>Detected</th>
                        </tr>
                        </thead>
                        <tbody>
                """
        );

        if (counts.isEmpty()) {

            html.append(
                    """
                    <tr>
                        <td colspan="2"
                            class="empty">
                            No privacy candidates detected.
                        </td>
                    </tr>
                    """
            );

        } else {

            counts.forEach(
                    (type, count) -> {

                        html.append("<tr>");

                        html.append("<td>");
                        html.append(
                                escape(
                                        type.name()
                                )
                        );
                        html.append("</td>");

                        html.append("<td>");
                        html.append(count);
                        html.append("</td>");

                        html.append("</tr>");
                    }
            );
        }

        html.append(
                """
                        </tbody>
                    </table>
                </div>
                """
        );
    }

    private void appendDataFlowSummary(
            StringBuilder html,
            AnalysisResult result
    ) {

        long databaseMapper =
                result.dataFlows()
                        .stream()
                        .filter(
                                edge ->
                                        edge.relation()
                                                == DataFlowRelation
                                                .DATABASE_MAPPER
                        )
                        .count();

        long mapperResult =
                result.dataFlows()
                        .stream()
                        .filter(
                                edge ->
                                        edge.relation()
                                                == DataFlowRelation
                                                .MAPPER_RESULT
                        )
                        .count();

        long apiResponse =
                result.dataFlows()
                        .stream()
                        .filter(
                                edge ->
                                        edge.relation()
                                                == DataFlowRelation
                                                .API_RESPONSE
                        )
                        .count();

        html.append(
                """
                <div class="section">
                    <h2 class="section-title">
                        Data Flow Summary
                    </h2>

                    <table>
                        <thead>
                        <tr>
                            <th>Relation</th>
                            <th>Detected Flows</th>
                        </tr>
                        </thead>
                        <tbody>
                """
        );

        appendFlowRow(
                html,
                "Database → Mapper",
                databaseMapper
        );

        appendFlowRow(
                html,
                "Mapper → Java DTO",
                mapperResult
        );

        appendFlowRow(
                html,
                "Java DTO → API",
                apiResponse
        );

        html.append(
                """
                        </tbody>
                    </table>
                </div>
                """
        );
    }

    private void appendFlowRow(
            StringBuilder html,
            String label,
            long count
    ) {

        html.append("<tr>");

        html.append("<td>");
        html.append(
                escape(label)
        );
        html.append("</td>");

        html.append("<td>");
        html.append(count);
        html.append("</td>");

        html.append("</tr>");
    }

    private void appendPolicies(
            StringBuilder html,
            AnalysisResult result
    ) {

        html.append(
                """
                <div class="section">
                    <h2 class="section-title">
                        Privacy Management Policies
                    </h2>
                """
        );

        if (result.policy()
                .resources()
                .isEmpty()) {

            html.append(
                    """
                    <div class="empty">
                        No resource policies loaded.
                    </div>
                    """
            );

        } else {

            html.append(
                    """
                    <table>
                        <thead>
                        <tr>
                            <th>Resource</th>
                            <th>Purpose</th>
                            <th>Retention</th>
                            <th>Disposal</th>
                        </tr>
                        </thead>
                        <tbody>
                    """
            );

            result.policy()
                    .resources()
                    .forEach(
                            (resourceName, policy) ->
                                    appendPolicyRow(
                                            html,
                                            resourceName,
                                            policy
                                    )
                    );

            html.append(
                    """
                        </tbody>
                    </table>
                    """
            );
        }

        html.append(
                """
                </div>
                """
        );
    }

    private void appendPolicyRow(
            StringBuilder html,
            String resourceName,
            ResourcePolicy policy
    ) {

        html.append("<tr>");

        html.append("<td>");
        html.append(
                escape(resourceName)
        );
        html.append("</td>");

        html.append("<td>");
        html.append(
                displayValue(
                        policy.purpose()
                )
        );
        html.append("</td>");

        html.append("<td>");
        html.append(
                displayValue(
                        policy.retention()
                )
        );
        html.append("</td>");

        html.append("<td>");
        html.append(
                displayValue(
                        policy.disposal()
                )
        );
        html.append("</td>");

        html.append("</tr>");
    }

    private void appendFindings(
            StringBuilder html,
            AnalysisResult result
    ) {

        html.append(
                """
                <div class="section">
                    <h2 class="section-title">
                        Findings
                    </h2>
                """
        );

        if (result.findings()
                .isEmpty()) {

            html.append(
                    """
                    <div class="empty">
                        No findings detected.
                    </div>
                    """
            );

        } else {

            for (Finding finding :
                    result.findings()) {

                html.append(
                        """
                        <div class="finding">
                            <span class="badge">
                        """
                );

                html.append(
                        escape(
                                finding.severity()
                                        .name()
                        )
                );

                html.append(
                        " · "
                );

                html.append(
                        escape(
                                finding.ruleId()
                        )
                );

                html.append(
                        """
                            </span>

                            <div class="finding-title">
                        """
                );

                html.append(
                        escape(
                                finding.title()
                        )
                );

                html.append(
                        """
                            </div>

                            <div class="finding-description">
                        """
                );

                html.append(
                        escape(
                                finding.description()
                        )
                );

                html.append(
                        """
                            </div>
                        """
                );

                if (finding.location() != null) {

                    html.append(
                            """
                            <div class="location">
                                Location:
                            """
                    );

                    html.append(
                            escape(
                                    finding.location()
                                            .file()
                            )
                    );

                    if (finding.location()
                            .line() != null) {

                        html.append(":");
                        html.append(
                                finding.location()
                                        .line()
                        );
                    }

                    html.append(
                            """
                            </div>
                            """
                    );
                }

                html.append(
                        """
                        </div>
                        """
                );
            }
        }

        html.append(
                """
                </div>
                """
        );
    }

    private Map<PrivacyType, Long>
    countPrivacyTypes(
            AnalysisResult result
    ) {

        Map<PrivacyType, Long> counts =
                new EnumMap<>(
                        PrivacyType.class
                );

        for (ClassifiedFact fact :
                result.classifiedFacts()) {

            PrivacyType type =
                    fact.classification()
                            .privacyType();

            counts.merge(
                    type,
                    1L,
                    Long::sum
            );
        }

        return counts;
    }

    private Map<Severity, Long>
    countSeverities(
            AnalysisResult result
    ) {

        Map<Severity, Long> counts =
                new EnumMap<>(
                        Severity.class
                );

        for (Finding finding :
                result.findings()) {

            counts.merge(
                    finding.severity(),
                    1L,
                    Long::sum
            );
        }

        return counts;
    }

    private String displayValue(
            String value
    ) {

        if (value == null
                || value.isBlank()) {

            return "NOT SET";
        }

        return escape(value);
    }

    private String escape(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}