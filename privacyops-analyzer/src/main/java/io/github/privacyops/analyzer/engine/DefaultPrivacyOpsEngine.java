package io.github.privacyops.analyzer.engine;

import io.github.privacyops.analyzer.PrivacyAnalysisService;
import io.github.privacyops.engine.PrivacyOpsEngine;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowLinker;
import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.Finding;
import io.github.privacyops.policy.PrivacyPolicy;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;
import io.github.privacyops.scan.ScanResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DefaultPrivacyOpsEngine
        implements PrivacyOpsEngine {

    private final PrivacyAnalysisService analysisService;
    private final List<DataFlowLinker> flowLinkers;
    private final List<PrivacyRule> rules;

    public DefaultPrivacyOpsEngine(
            PrivacyAnalysisService analysisService,
            List<DataFlowLinker> flowLinkers,
            List<PrivacyRule> rules
    ) {
        this.analysisService =
                analysisService;

        this.flowLinkers =
                List.copyOf(flowLinkers);

        this.rules =
                List.copyOf(rules);
    }

    @Override
    public AnalysisResult analyze(
            Path projectRoot,
            PrivacyPolicy policy
    ) {

        ScanResult scanResult =
                analysisService.scan(
                        projectRoot
                );

        List<ClassifiedFact> classifiedFacts =
                analysisService.classify(
                        scanResult.facts()
                );

        List<DataFlowEdge> dataFlows =
                new ArrayList<>();

        for (DataFlowLinker linker :
                flowLinkers) {

            dataFlows.addAll(
                    linker.link(
                            scanResult.facts()
                    )
            );
        }

        RuleContext ruleContext =
                new RuleContext(
                        scanResult.facts(),
                        classifiedFacts,
                        dataFlows,
                        policy
                );

        List<Finding> findings =
                rules.stream()
                        .flatMap(
                                rule ->
                                        rule.evaluate(
                                                ruleContext
                                        ).stream()
                        )
                        .toList();

        return new AnalysisResult(
                scanResult.facts(),
                classifiedFacts,
                List.copyOf(dataFlows),
                findings,
                scanResult.warnings(),
                policy
        );
    }
}