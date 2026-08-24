package io.github.privacyops.access;

import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.rule.RuleContext;

public interface AccessControlProvider {

    /**
     * Provider의 고유 식별자.
     *
     * 예:
     * spring-security
     * organization-iam
     * external-hr-access
     */
    String id();

    /**
     * 해당 endpoint/context를
     * 이 Provider가 평가할 수 있는지 판단한다.
     */
    boolean supports(
            ApiEndpointFact endpoint,
            RuleContext context
    );

    /**
     * 실제 접근통제 존재 여부를 평가한다.
     */
    AccessControlAssessment inspect(
            ApiEndpointFact endpoint,
            RuleContext context
    );
}