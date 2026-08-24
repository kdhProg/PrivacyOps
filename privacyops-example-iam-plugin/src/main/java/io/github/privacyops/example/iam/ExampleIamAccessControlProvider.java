package io.github.privacyops.example.iam;

import io.github.privacyops.access.AccessControlAssessment;
import io.github.privacyops.access.AccessControlProvider;
import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.model.Evidence;
import io.github.privacyops.model.EvidenceType;
import io.github.privacyops.rule.RuleContext;

import java.util.List;

public class ExampleIamAccessControlProvider
        implements AccessControlProvider {

    public static final String PROVIDER_ID =
            "example-iam";

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
         * Example Plugin:
         *
         * 실제 조직에서는 여기에서
         * endpoint/API 정보가 IAM 연계 대상인지
         * 판단할 수 있다.
         *
         * 데모에서는 /members API만
         * 외부 IAM 관리 대상으로 간주한다.
         */
        return endpoint != null
                && endpoint.path() != null
                && endpoint.path()
                .startsWith(
                        "/members"
                );
    }

    @Override
    public AccessControlAssessment inspect(
            ApiEndpointFact endpoint,
            RuleContext context
    ) {

        /*
         * 실제 구현 예:
         *
         * IAM API
         * HR System
         * API Gateway
         * Authorization Server
         *
         * 등에 질의하여 권한 설정 여부를
         * 확인할 수 있다.
         *
         * MVP Plugin에서는 외부 IAM에 의해
         * 보호되는 것으로 가정한다.
         */

        return AccessControlAssessment
                .controlled(
                        id(),
                        "Access control verified by "
                                + "example external IAM provider.",
                        List.of(
                                new Evidence(
                                        EvidenceType
                                                .EXTERNAL_PROVIDER,
                                        "External IAM",
                                        "Endpoint "
                                                + endpoint.httpMethod()
                                                + " "
                                                + endpoint.path()
                                                + " is protected by "
                                                + "the example IAM provider.",
                                        endpoint.location()
                                )
                        )
                );
    }
}