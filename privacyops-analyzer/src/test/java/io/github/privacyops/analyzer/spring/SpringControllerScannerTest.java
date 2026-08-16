package io.github.privacyops.analyzer.spring;

import io.github.privacyops.fact.ApiEndpointFact;
import io.github.privacyops.fact.Fact;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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

        assertEquals(
                1,
                facts.size()
        );

        ApiEndpointFact endpoint =
                (ApiEndpointFact) facts.get(0);

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
}