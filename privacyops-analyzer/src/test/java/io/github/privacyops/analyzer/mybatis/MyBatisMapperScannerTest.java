package io.github.privacyops.analyzer.mybatis;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.MapperQueryFact;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MyBatisMapperScannerTest {

    private final MyBatisMapperScanner scanner =
            new MyBatisMapperScanner();

    // 10-5
    @Test
    void extractsSelectStatement()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample/"
                                                + "src/main/resources/"
                                                + "mapper/"
                                                + "MemberMapper.xml"
                                )
                                .toURI()
                );

        List<Fact> facts =
                scanner.scan(path);

        assertEquals(
                1,
                facts.size()
        );

        MapperQueryFact fact =
                (MapperQueryFact) facts.get(0);

        assertEquals(
                "samples.MemberMapper.selectMember",
                fact.mapperId()
        );

        assertEquals(
                "samples.MemberDto",
                fact.resultType()
        );

        assertEquals(
                List.of("TB_MEMBER"),
                fact.tables()
        );

        assertTrue(
                fact.columns().contains("RRN")
        );

        assertTrue(
                fact.columns().contains("EMAIL_ADDR")
        );

        assertTrue(
                fact.columns().contains("PHONE_NO")
        );
    }

    // 10-6
    @Test
    void ignoresNonMapperXml()
            throws Exception {

        Path path =
                Path.of(
                        getClass()
                                .getClassLoader()
                                .getResource(
                                        "project-sample/"
                                                + "src/main/resources/"
                                                + "application-context.xml"
                                )
                                .toURI()
                );

        List<Fact> facts =
                scanner.scan(path);

        assertTrue(
                facts.isEmpty()
        );
    }
}