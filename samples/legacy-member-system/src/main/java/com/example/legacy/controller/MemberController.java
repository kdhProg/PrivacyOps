package com.example.legacy.controller;

import com.example.legacy.dto.MemberDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/members")
public class MemberController {


    @PreAuthorize("hasRole('MEMBER_MANAGER')")
    @PrivacyAudit("MEMBER_READ")
    @GetMapping("/{id}")
    public MemberDto getMember(
            @PathVariable Long id
    ) {

        return null;
    }
}