package io.github.privacyops.analyzer.engine;

import io.github.privacyops.analyzer.PrivacyAnalysisService;
import io.github.privacyops.engine.PrivacyOpsEngine;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.flow.DataFlowEdge;
import io.github.privacyops.flow.DataFlowLinker;
import io.github.privacyops.model.AnalysisResult;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.model.Finding;
import io.github.privacyops.policy.PrivacyPolicy;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rule.RuleContext;
import io.github.privacyops.scan.DatabaseScanner;
import io.github.privacyops.scan.ScanResult;

import java.nio.file.Path;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class DefaultPrivacyOpsEngine
        implements PrivacyOpsEngine {

    private final PrivacyAnalysisService analysisService;
    private final List<DataFlowLinker> flowLinkers;
    private final List<PrivacyRule> rules;
    private final DatabaseScanner databaseScanner;

    public DefaultPrivacyOpsEngine(
            PrivacyAnalysisService analysisService,
            DatabaseScanner databaseScanner,
            List<DataFlowLinker> flowLinkers,
            List<PrivacyRule> rules
    ) {
        this.analysisService =
                analysisService;

        this.databaseScanner =
                databaseScanner;

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

        return analyzeInternal(
                projectRoot,
                policy,
                List.of()
        );
    }

    @Override
    public AnalysisResult analyze(
            Path projectRoot,
            PrivacyPolicy policy,
            Connection databaseConnection,
            String databaseSchema
    ) {

        List<Fact> databaseFacts =
                databaseScanner.scan(
                        databaseConnection,
                        databaseSchema
                );

        return analyzeInternal(
                projectRoot,
                policy,
                databaseFacts
        );
    }

    private AnalysisResult analyzeInternal(
            Path projectRoot,
            PrivacyPolicy policy,
            List<Fact> additionalFacts
    ) {

        ScanResult scanResult =
                analysisService.scan(
                        projectRoot
                );

        List<Fact> allFacts =
                new ArrayList<>(
                        scanResult.facts()
                );

        allFacts.addAll(
                additionalFacts
        );

        List<ClassifiedFact> classifiedFacts =
                analysisService.classify(
                        allFacts
                );

        List<DataFlowEdge> dataFlows =
                new ArrayList<>();

        for (DataFlowLinker linker :
                flowLinkers) {

            dataFlows.addAll(
                    linker.link(
                            allFacts
                    )
            );
        }

        RuleContext ruleContext =
                new RuleContext(
                        allFacts,
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
                List.copyOf(allFacts),
                classifiedFacts,
                List.copyOf(dataFlows),
                findings,
                scanResult.warnings(),
                policy
        );
    }
}