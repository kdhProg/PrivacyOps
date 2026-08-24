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
import io.github.privacyops.analyzer.rulepack.ConfiguredPrivacyRule;
import io.github.privacyops.analyzer.spring.SpringControllerScanner;
import io.github.privacyops.engine.PrivacyOpsEngine;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rulepack.RulePack;

import java.util.ArrayList;
import java.util.List;

public final class PrivacyOpsEngineFactory {

    private PrivacyOpsEngineFactory() {
    }

    public static PrivacyOpsEngine createDefault() {

        return createDefault(
                RulePack.defaults()
        );
    }

    public static PrivacyOpsEngine createDefault(
            RulePack rulePack
    ) {

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

        List<PrivacyRule> baseRules =
                List.of(
                        new ApiPrivacyExposureRule(),
                        new MissingAccessControlRule(),
                        new MissingAuditControlRule(),
                        new MissingResourcePolicyRule(),
                        new MissingRetentionPolicyRule(),
                        new MissingDisposalPolicyRule()
                );

        List<PrivacyRule> configuredRules =
                new ArrayList<>();

        for (PrivacyRule rule :
                baseRules) {

            configuredRules.add(
                    new ConfiguredPrivacyRule(
                            rule,
                            rulePack.settingFor(
                                    rule.id()
                            )
                    )
            );
        }

        return new DefaultPrivacyOpsEngine(
                analysisService,
                new JdbcDatabaseScanner(),
                List.of(
                        new DatabaseMapperFlowLinker(),
                        new MapperResultFlowLinker(),
                        new ControllerResponseFlowLinker()
                ),
                configuredRules
        );
    }
}