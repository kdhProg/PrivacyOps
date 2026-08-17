package sample;

import io.github.privacyops.api.PrivacyAudit;

public class LegacyMemberService {

    @PrivacyAudit(
            "PERSONAL_INFO_VIEW"
    )
    public LegacyMemberDto findMember(
            Long memberId
    ) {

        return null;
    }
}