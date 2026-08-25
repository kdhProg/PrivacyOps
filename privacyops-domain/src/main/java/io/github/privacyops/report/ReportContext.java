package io.github.privacyops.report;

import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.risk.RiskAssessment;

import java.util.List;

public record ReportContext(
        AnalysisResult analysisResult,
        List<RiskAssessment> riskAssessments
) {

    public ReportContext {

        if (analysisResult == null) {
            throw new IllegalArgumentException(
                    "Analysis result must not be null."
            );
        }

        riskAssessments =
                riskAssessments == null
                        ? List.of()
                        : List.copyOf(
                        riskAssessments
                );
    }

    public static ReportContext of(
            AnalysisResult analysisResult
    ) {

        return new ReportContext(
                analysisResult,
                List.of()
        );
    }
}