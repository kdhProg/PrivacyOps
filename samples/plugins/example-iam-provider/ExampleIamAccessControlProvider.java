public class ExampleIamAccessControlProvider
        implements AccessControlProvider {

    @Override
    public String id() {
        return "example-iam";
    }

    @Override
    public boolean supports(
            ApiEndpointFact endpoint,
            RuleContext context
    ) {
        return true;
    }

    @Override
    public AccessControlAssessment inspect(
            ApiEndpointFact endpoint,
            RuleContext context
    ) {

        /*
         * 실제 환경에서는
         * IAM / HR / 권한관리 API 조회 가능
         */

        return new AccessControlAssessment(
                true,
                id(),
                "Access control verified by external IAM.",
                List.of(...)
        );
    }
}