package samples;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecuredMemberController {

    @PreAuthorize("hasRole('PRIVACY_HANDLER')")
    @GetMapping("/members/{id}")
    public MemberDto getMember() {
        return null;
    }
}