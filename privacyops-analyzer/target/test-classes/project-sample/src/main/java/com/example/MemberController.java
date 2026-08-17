package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/members")
public class MemberController {

    @PreAuthorize(
            "hasRole('PRIVACY_HANDLER')"
    )
    @PrivacyAudit(
            "PERSONAL_INFO_VIEW"
    )
    @GetMapping("/{id}")
    public MemberDto getMember() {
        return null;
    }
}