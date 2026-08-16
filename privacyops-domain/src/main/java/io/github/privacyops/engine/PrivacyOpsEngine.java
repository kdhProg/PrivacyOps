package io.github.privacyops.engine;

import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.policy.PrivacyPolicy;

import java.nio.file.Path;

public interface PrivacyOpsEngine {

    AnalysisResult analyze(
            Path projectRoot,
            PrivacyPolicy policy
    );
}