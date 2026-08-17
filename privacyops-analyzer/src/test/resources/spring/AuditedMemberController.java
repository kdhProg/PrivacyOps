package samples;

import io.github.privacyops.api.PrivacyAudit;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditedMemberController {

    @PrivacyAudit("PERSONAL_INFO_VIEW")
    @GetMapping("/members/{id}")
    public MemberDto getMember() {
        return null;
    }
}