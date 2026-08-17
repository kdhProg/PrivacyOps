package io.github.privacyops.analyzer.spring;

import io.github.privacyops.fact.ApiAccessControlFact;
import io.github.privacyops.fact.ApiAuditControlFact;
import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.Fact;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringControllerScannerTest {

    private final SpringControllerScanner scanner =
            new SpringControllerScanner();

    @Test
    void extractsSpringApiEndpoint()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample/"
                                                + "src/main/java/"
                                                + "com/example/"
                                                + "MemberController.java"
                                )
                                .toURI()
                );

        List<Fact> facts =
                scanner.scan(path);

//        assertEquals(
//                1,
//                facts.size()
//        );
//
//        ApiEndpointFact endpoint =
//                (ApiEndpointFact) facts.get(0);
        ApiEndpointFact endpoint =
                facts.stream()
                        .filter(
                                ApiEndpointFact.class
                                        ::isInstance
                        )
                        .map(
                                ApiEndpointFact.class
                                        ::cast
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "GET",
                endpoint.httpMethod()
        );

        assertEquals(
                "/members/{id}",
                endpoint.path()
        );

        assertEquals(
                "MemberDto",
                endpoint.responseType()
        );

        assertEquals(
                "getMember",
                endpoint.controllerMethod()
        );
    }

    @Test
    void extractsAccessControlAnnotation()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "spring/"
                                                + "SecuredMemberController.java"
                                )
                                .toURI()
                );

        List<Fact> facts =
                scanner.scan(path);

        ApiAccessControlFact access =
                facts.stream()
                        .filter(
                                ApiAccessControlFact.class
                                        ::isInstance
                        )
                        .map(
                                ApiAccessControlFact.class
                                        ::cast
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "PreAuthorize",
                access.annotationType()
        );

        assertTrue(
                access.expression()
                        .contains(
                                "PRIVACY_HANDLER"
                        )
        );
    }

    @Test
    void extractsPrivacyAuditAnnotation()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "spring/"
                                                + "AuditedMemberController.java"
                                )
                                .toURI()
                );

        List<Fact> facts =
                scanner.scan(path);

        ApiAuditControlFact audit =
                facts.stream()
                        .filter(
                                ApiAuditControlFact.class
                                        ::isInstance
                        )
                        .map(
                                ApiAuditControlFact.class
                                        ::cast
                        )
                        .findFirst()
                        .orElseThrow();

        assertEquals(
                "PERSONAL_INFO_VIEW",
                audit.auditEvent()
        );

        assertEquals(
                "PrivacyAudit",
                audit.sourceType()
        );

    }
}