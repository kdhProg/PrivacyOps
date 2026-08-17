package samples;

import io.github.privacyops.api.PrivacyData;

public class MemberDto {

    private Long id;

    private String name;

    private String rrn;

    private String emailAddress;

    private String phoneNumber;

    @PrivacyData(
            type = "NATIONAL_IDENTIFIER"
    )
    private String secretValue;
}