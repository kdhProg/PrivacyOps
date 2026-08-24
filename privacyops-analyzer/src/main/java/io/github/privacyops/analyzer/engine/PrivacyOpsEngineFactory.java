package io.github.privacyops.analyzer.engine;

import io.github.privacyops.access.AccessControlProvider;
import io.github.privacyops.analyzer.PrivacyAnalysisService;
import io.github.privacyops.analyzer.access.AccessControlProviderLoader;
import io.github.privacyops.analyzer.classifier.NamePatternPrivacyClassifier;
import io.github.privacyops.analyzer.classifier.RiskProfilePrivacyClassifier;
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
import io.github.privacyops.risk.RiskProfile;
import io.github.privacyops.rule.PrivacyRule;
import io.github.privacyops.rulepack.RulePack;

import java.util.ArrayList;
import java.util.List;

public final class PrivacyOpsEngineFactory {

    private PrivacyOpsEngineFactory() {
    }

    /**
     * PrivacyOps 기본 설정.
     *
     * - 기본 Rule Pack
     * - 기본 Risk Profile
     */
    public static PrivacyOpsEngine createDefault() {

        return createDefault(
                RulePack.defaults(),
                RiskProfile.defaults()
        );
    }

    /**
     * Rule Pack만 사용자 정의.
     *
     * Risk Profile은 기본값 사용.
     */
    public static PrivacyOpsEngine createDefault(
            RulePack rulePack
    ) {

        return createDefault(
                rulePack,
                RiskProfile.defaults()
        );
    }

    /**
     * Rule Pack + Risk Profile 사용자 정의.
     */
    public static PrivacyOpsEngine createDefault(
            RulePack rulePack,
            RiskProfile riskProfile
    ) {

        RulePack effectiveRulePack =
                rulePack == null
                        ? RulePack.defaults()
                        : rulePack;

        RiskProfile effectiveRiskProfile =
                riskProfile == null
                        ? RiskProfile.defaults()
                        : riskProfile;

        // 1. Project Scanner 구성
        DefaultProjectScanner projectScanner =
                new DefaultProjectScanner(
                        List.of(
                                new JavaSourceScanner(),
                                new SpringControllerScanner(),
                                new MyBatisMapperScanner()
                        )
                );

        // 2. Privacy Classifier 구성
        PrivacyAnalysisService analysisService =
                new PrivacyAnalysisService(
                        projectScanner,
                        List.of(
                                /*
                                 * 사용자 정의 Risk Profile의
                                 * keyword classifier를 먼저 적용한다.
                                 */
                                new RiskProfilePrivacyClassifier(
                                        effectiveRiskProfile
                                ),

                                /*
                                 * PrivacyOps 기본 이름 기반 분류기
                                 */
                                new NamePatternPrivacyClassifier()
                        )
                );

        // 3. Access Control Provider 로딩
        List<AccessControlProvider>
                accessControlProviders =
                new AccessControlProviderLoader()
                        .load();

        // 4. 기본 Privacy Rule 구성
        List<PrivacyRule> baseRules =
                List.of(
                        new ApiPrivacyExposureRule(),

                        new MissingAccessControlRule(
                                accessControlProviders
                        ),

                        new MissingAuditControlRule(),
                        new MissingResourcePolicyRule(),
                        new MissingRetentionPolicyRule(),
                        new MissingDisposalPolicyRule()
                );

        // 5. Rule Pack 적용
        List<PrivacyRule> configuredRules =
                new ArrayList<>();

        for (PrivacyRule rule :
                baseRules) {

            configuredRules.add(
                    new ConfiguredPrivacyRule(
                            rule,
                            effectiveRulePack.settingFor(
                                    rule.id()
                            )
                    )
            );
        }

        // 6. Engine 생성
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