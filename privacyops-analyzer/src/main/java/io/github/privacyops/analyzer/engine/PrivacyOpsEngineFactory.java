package io.github.privacyops.analyzer.engine;

import io.github.privacyops.analyzer.PrivacyAnalysisService;
import io.github.privacyops.analyzer.classifier.NamePatternPrivacyClassifier;
import io.github.privacyops.analyzer.flow.ControllerResponseFlowLinker;
import io.github.privacyops.analyzer.flow.MapperResultFlowLinker;
import io.github.privacyops.analyzer.java.JavaSourceScanner;
import io.github.privacyops.analyzer.mybatis.MyBatisMapperScanner;
import io.github.privacyops.analyzer.project.DefaultProjectScanner;
import io.github.privacyops.analyzer.rule.*;
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
                List.of(
                        new MapperResultFlowLinker(),
                        new ControllerResponseFlowLinker()
                ),
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