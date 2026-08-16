package io.github.privacyops.analyzer.engine;

import io.github.privacyops.engine.PrivacyOpsEngine;
import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.policy.PrivacyPolicy;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DefaultPrivacyOpsEngineTest {

    @Test
    void analyzesProjectEndToEnd()
            throws Exception {

        Path projectRoot =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample"
                                )
                                .toURI()
                );

        PrivacyOpsEngine engine =
                PrivacyOpsEngineFactory
                        .createDefault();

        AnalysisResult result =
                engine.analyze(
                        projectRoot,
                        PrivacyPolicy.empty()
                );

        assertFalse(
                result.facts()
                        .isEmpty()
        );

        assertFalse(
                result.classifiedFacts()
                        .isEmpty()
        );

        assertFalse(
                result.dataFlows()
                        .isEmpty()
        );

        assertFalse(
                result.findings()
                        .isEmpty()
        );
    }

    @Test
    void managedResourceDoesNotProducePolicyFindings()
            throws Exception {

        Path projectRoot =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample"
                                )
                                .toURI()
                );

        Path policyPath =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample/"
                                                + "privacyops-policy.yml"
                                )
                                .toURI()
                );

        var policyProvider =
                new io.github.privacyops.analyzer.policy
                        .YamlPolicyProvider();

        var policy =
                policyProvider.load(
                        policyPath
                );

        PrivacyOpsEngine engine =
                PrivacyOpsEngineFactory
                        .createDefault();

        AnalysisResult result =
                engine.analyze(
                        projectRoot,
                        policy
                );

        boolean policyFindingExists =
                result.findings()
                        .stream()
                        .anyMatch(
                                finding ->
                                        finding.ruleId()
                                                .startsWith(
                                                        "PRIV-POLICY"
                                                )
                                                || finding.ruleId()
                                                .startsWith(
                                                        "PRIV-RETENTION"
                                                )
                                                || finding.ruleId()
                                                .startsWith(
                                                        "PRIV-DISPOSAL"
                                                )
                        );

        assertFalse(
                policyFindingExists
        );
    }
}