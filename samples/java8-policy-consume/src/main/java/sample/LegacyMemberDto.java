package sample;

import io.github.privacyops.api.PrivacyData;

public class LegacyMemberDto {

    private Long memberId;

    @PrivacyData(
            type = "NAME"
    )
    private String name;

    @PrivacyData(
            type = "NATIONAL_IDENTIFIER"
    )
    private String rrn;

    @PrivacyData(
            type = "EMAIL"
    )
    private String emailAddress;

    public Long getMemberId() {
        return memberId;
    }

    public void setMemberId(
            Long memberId
    ) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(
            String name
    ) {
        this.name = name;
    }

    public String getRrn() {
        return rrn;
    }

    public void setRrn(
            String rrn
    ) {
        this.rrn = rrn;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(
            String emailAddress
    ) {
        this.emailAddress =
                emailAddress;
    }
}