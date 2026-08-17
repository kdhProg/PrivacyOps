package io.github.privacyops.analyzer;

import io.github.privacyops.analyzer.classifier.AnnotationPrivacyClassifier;
import io.github.privacyops.classifier.PrivacyClassifier;
import io.github.privacyops.fact.Fact;
import io.github.privacyops.model.ClassifiedFact;
import io.github.privacyops.scan.ProjectScanner;
import io.github.privacyops.scan.ScanResult;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PrivacyAnalysisService {

    private final ProjectScanner projectScanner;

    private final List<PrivacyClassifier> classifiers;

    private final AnnotationPrivacyClassifier
            annotationPrivacyClassifier;

    /**
     * 기존 코드와의 호환을 위한 생성자.
     *
     * 기존 PrivacyOpsEngineFactory에서
     * new PrivacyAnalysisService(
     *     projectScanner,
     *     List.of(...)
     * )
     * 형태를 그대로 사용할 수 있다.
     */
    public PrivacyAnalysisService(
            ProjectScanner projectScanner,
            List<PrivacyClassifier> classifiers
    ) {

        this(
                projectScanner,
                classifiers,
                new AnnotationPrivacyClassifier()
        );
    }

    /**
     * Annotation classifier까지 외부에서
     * 주입할 수 있는 생성자.
     */
    public PrivacyAnalysisService(
            ProjectScanner projectScanner,
            List<PrivacyClassifier> classifiers,
            AnnotationPrivacyClassifier
                    annotationPrivacyClassifier
    ) {

        this.projectScanner =
                projectScanner;

        this.classifiers =
                List.copyOf(
                        classifiers
                );

        this.annotationPrivacyClassifier =
                annotationPrivacyClassifier;
    }

    public ScanResult scan(
            Path root
    ) {

        return projectScanner.scan(
                root
        );
    }

    public List<ClassifiedFact> classify(
            List<Fact> facts
    ) {

        /*
         * key   = 원본 Fact ID
         * value = 최종 Classification
         *
         * LinkedHashMap을 사용하는 이유:
         * - 중복 Classification 방지
         * - 입력/분류 순서를 최대한 유지
         */
        Map<String, ClassifiedFact> results =
                new LinkedHashMap<>();

        /*
         * 1. 기본 자동 분류
         *
         * 현재는 예:
         * NamePatternPrivacyClassifier
         *
         * rrn         -> NATIONAL_IDENTIFIER
         * email       -> EMAIL
         * phoneNumber -> PHONE_NUMBER
         */
        for (Fact fact :
                facts) {

            for (PrivacyClassifier classifier :
                    classifiers) {

                classifier.classify(
                                fact
                        )
                        .ifPresent(
                                classification ->
                                        results.putIfAbsent(
                                                fact.id(),
                                                new ClassifiedFact(
                                                        fact,
                                                        classification
                                                )
                                        )
                        );
            }
        }

        /*
         * 2. @PrivacyData 기반 명시적 분류
         *
         * Annotation은 개발자가 개인정보 유형을
         * 명시적으로 선언한 것이므로
         * 이름 기반 추정보다 우선한다.
         *
         * 따라서 put()을 사용해
         * 동일 Fact의 기존 Classification을
         * 덮어쓴다.
         */
        List<ClassifiedFact> annotationResults =
                annotationPrivacyClassifier
                        .classify(
                                facts
                        );

        for (ClassifiedFact classifiedFact :
                annotationResults) {

            results.put(
                    classifiedFact
                            .fact()
                            .id(),
                    classifiedFact
            );
        }

        return List.copyOf(
                results.values()
        );
    }
}