package io.github.privacyops.analyzer.engine;

import io.github.privacyops.analyzer.PrivacyAnalysisService;
import io.github.privacyops.analyzer.classifier.NamePatternPrivacyClassifier;
import io.github.privacyops.analyzer.database.JdbcDatabaseScanner;
import io.github.privacyops.analyzer.flow.ControllerResponseFlowLinker;
import io.github.privacyops.analyzer.flow.DatabaseMapperFlowLinker;
import io.github.privacyops.analyzer.flow.MapperResultFlowLinker;
import io.github.privacyops.analyzer.java.JavaSourceScanner;
import io.github.privacyops.analyzer.mybatis.MyBatisMapperScanner;
import io.github.privacyops.analyzer.project.DefaultProjectScanner;
import io.github.privacyops.analyzer.rule.ApiPrivacyExposureRule;
import io.github.privacyops.analyzer.rule.MissingAccessControlRule;
import io.github.privacyops.analyzer.rule.MissingAuditControlRule;
import io.github.privacyops.analyzer.rule.MissingDisposalPolicyRule;
import io.github.privacyops.analyzer.rule.MissingResourcePolicyRule;
import io.github.privacyops.analyzer.rule.MissingRetentionPolicyRule;
import io.github.privacyops.analyzer.spring.SpringControllerScanner;
import io.github.privacyops.engine.PrivacyOpsEngine;

import java.util.List;

public final class PrivacyOpsEngineFactory {

    private PrivacyOpsEngineFactory() {
    }

    public static PrivacyOpsEngine createDefault() {

        DefaultProjectScanner projectScanner =
                new DefaultProjectScanner(
                        List.of(
                                new JavaSourceScanner(),
                                new SpringControllerScanner(),
                                new MyBatisMapperScanner()
                        )
                );

        PrivacyAnalysisService analysisService =
                new PrivacyAnalysisService(
                        projectScanner,
                        List.of(
                                new NamePatternPrivacyClassifier()
                        )
                );

        return new DefaultPrivacyOpsEngine(
                analysisService,

                // DB Metadata Scanner
                new JdbcDatabaseScanner(),

                // Data Flow Linkers
                List.of(
                        new DatabaseMapperFlowLinker(),
                        new MapperResultFlowLinker(),
                        new ControllerResponseFlowLinker()
                ),

                // Privacy Rules
                List.of(
                        new ApiPrivacyExposureRule(),
                        new MissingAccessControlRule(),
                        new MissingAuditControlRule(),
                        new MissingResourcePolicyRule(),
                        new MissingRetentionPolicyRule(),
                        new MissingDisposalPolicyRule()
                )
        );
    }
}