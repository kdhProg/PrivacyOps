package samples;

import io.github.privacyops.api.PrivacyData;

public class AnnotatedPrivacyDto {

    @PrivacyData(
            type = "NATIONAL_IDENTIFIER"
    )
    private String secretValue;

    private String ordinaryValue;
}