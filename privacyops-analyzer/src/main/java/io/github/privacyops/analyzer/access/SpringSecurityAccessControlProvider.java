package io.github.privacyops.analyzer.access;

import io.github.privacyops.access.AccessControlAssessment;
import io.github.privacyops.access.AccessControlProvider;
import io.github.privacyops.fact.ApiAccessControlFact;
import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.model.Evidence;
import io.github.privacyops.model.EvidenceType;
import io.github.privacyops.rule.RuleContext;

import java.util.List;

public class SpringSecurityAccessControlProvider
        implements AccessControlProvider {

    public static final String PROVIDER_ID =
            "spring-security";

    @Override
    public String id() {

        return PROVIDER_ID;
    }

    @Override
    public boolean supports(
            ApiEndpointFact endpoint,
            RuleContext context
    ) {

        /*
         * 현재 MVP에서는 Spring Controller Scanner가
         * 생성한 ApiEndpointFact를 대상으로 하므로
         * 모든 API Endpoint 평가를 시도한다.
         *
         * 향후 framework/platform 정보가 Fact에 들어오면
         * 여기서 Spring endpoint 여부를 구체적으로
         * 판단할 수 있다.
         */
        return endpoint != null
                && context != null;
    }

    @Override
    public AccessControlAssessment inspect(
            ApiEndpointFact endpoint,
            RuleContext context
    ) {

        ApiAccessControlFact accessControl =
                context.facts()
                        .stream()
                        .filter(
                                ApiAccessControlFact.class
                                        ::isInstance
                        )
                        .map(
                                ApiAccessControlFact.class
                                        ::cast
                        )
                        .filter(
                                access ->
                                        access.endpointId()
                                                .equals(
                                                        endpoint.id()
                                                )
                        )
                        .findFirst()
                        .orElse(null);

        if (accessControl == null) {

            return AccessControlAssessment
                    .notControlled(
                            id(),
                            "No Spring Security access control "
                                    + "was detected for this endpoint.",
                            List.of(
                                    new Evidence(
                                            EvidenceType.SOURCE_CODE,
                                            "Spring Security access control",
                                            "No recognized Spring Security "
                                                    + "access control was found.",
                                            endpoint.location()
                                    )
                            )
                    );
        }

        return AccessControlAssessment
                .controlled(
                        id(),
                        "Spring Security access control "
                                + "was detected for this endpoint.",
                        List.of(
                                new Evidence(
                                        EvidenceType.SOURCE_CODE,
                                        "Spring Security access control",
                                        "Recognized access control "
                                                + "declaration detected.",
                                        endpoint.location()
                                )
                        )
                );
    }
}