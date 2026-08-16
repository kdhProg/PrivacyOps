package io.github.privacyops.analyzer.mybatis;

import io.github.privacyops.fact.Fact;
import io.github.privacyops.fact.MapperColumnFact;
import io.github.privacyops.fact.MapperQueryFact;
import io.github.privacyops.model.SourceLocation;
import io.github.privacyops.scan.ArtifactScanner;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MyBatisMapperScanner
        implements ArtifactScanner {

    @Override
    public boolean supports(Path path) {

        if (path == null) {
            return false;
        }

        String value =
                path.toString()
                        .toLowerCase(Locale.ROOT);

        return value.endsWith(".xml");
    }

    @Override
    public List<Fact> scan(Path path) {

        List<Fact> facts =
                new ArrayList<>();

        try {

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory
                            .newInstance();

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );

            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            factory.setNamespaceAware(false);

            DocumentBuilder builder =
                    factory.newDocumentBuilder();

            Document document =
                    builder.parse(
                            path.toFile()
                    );

            Element root =
                    document.getDocumentElement();

            if (root == null
                    || !"mapper".equals(
                    root.getTagName())) {

                return facts;
            }

            String namespace =
                    root.getAttribute(
                            "namespace"
                    );

            NodeList children =
                    root.getChildNodes();

            for (int i = 0;
                 i < children.getLength();
                 i++) {

                Node node =
                        children.item(i);

                if (!(node instanceof Element element)) {
                    continue;
                }

                if (!"select".equals(
                        element.getTagName())) {

                    continue;
                }

                MapperQueryFact fact =
                        extractSelectFact(
                                path,
                                namespace,
                                element
                        );

                if (fact != null) {

                    facts.add(fact);

                    facts.addAll(
                            createColumnFacts(fact)
                    );
                }
            }

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to parse MyBatis mapper: "
                            + path,
                    e
            );
        }

        return facts;
    }

    private MapperQueryFact extractSelectFact(
            Path path,
            String namespace,
            Element select
    ) {

        String id =
                select.getAttribute("id");

        if (id == null || id.isBlank()) {
            return null;
        }

        String resultType =
                select.getAttribute(
                        "resultType"
                );

        String sql =
                select.getTextContent();

        List<String> tables =
                extractTables(sql);

        List<String> columns =
                extractColumns(sql);

        String mapperId =
                namespace == null
                        || namespace.isBlank()
                        ? id
                        : namespace + "." + id;

        return new MapperQueryFact(
                "mybatis:" + mapperId,
                mapperId,
                resultType,
                tables,
                columns,
                new SourceLocation(
                        path.toString(),
                        null
                )
        );
    }

    private List<String> extractTables(
            String sql
    ) {

        String normalized =
                normalizeSql(sql);

        String upper =
                normalized
                        .toUpperCase(
                                Locale.ROOT
                        );

        int fromIndex =
                upper.indexOf(" FROM ");

        if (fromIndex < 0) {
            return List.of();
        }

        String afterFrom =
                normalized.substring(
                        fromIndex + 6
                );

        String table =
                afterFrom
                        .split("\\s+")[0]
                        .trim();

        if (table.isBlank()) {
            return List.of();
        }

        return List.of(table);
    }

    private List<String> extractColumns(
            String sql
    ) {

        String normalized =
                normalizeSql(sql);

        String upper =
                normalized.toUpperCase(
                        Locale.ROOT
                );

        int selectIndex =
                upper.indexOf("SELECT ");

        int fromIndex =
                upper.indexOf(" FROM ");

        if (selectIndex < 0
                || fromIndex < 0
                || fromIndex <= selectIndex) {

            return List.of();
        }

        String columnPart =
                normalized.substring(
                        selectIndex + 7,
                        fromIndex
                );

        return Arrays.stream(
                        columnPart.split(",")
                )
                .map(String::trim)
                .filter(value ->
                        !value.isBlank()
                )
                .map(this::removeAlias)
                .toList();
    }

    private String removeAlias(
            String value
    ) {

        String upper =
                value.toUpperCase(
                        Locale.ROOT
                );

        int asIndex =
                upper.indexOf(" AS ");

        if (asIndex >= 0) {

            return value
                    .substring(
                            0,
                            asIndex
                    )
                    .trim();
        }

        String[] tokens =
                value.split("\\s+");

        return tokens[0].trim();
    }

    private String normalizeSql(
            String sql
    ) {

        if (sql == null) {
            return "";
        }

        return sql
                .replaceAll("\\s+", " ")
                .trim();
    }

    private List<Fact> createColumnFacts(
            MapperQueryFact query
    ) {

        if (query.tables().isEmpty()
                || query.columns().isEmpty()) {

            return List.of();
        }

        // 현재 MVP는 단일 테이블 SELECT만 지원
        String tableName =
                query.tables().get(0);

        List<Fact> facts =
                new ArrayList<>();

        for (String column :
                query.columns()) {

            String id =
                    "mybatis-column:"
                            + query.mapperId()
                            + "#"
                            + column;

            facts.add(
                    new MapperColumnFact(
                            id,
                            query.mapperId(),
                            tableName,
                            column,
                            query.resultType(),
                            query.location()
                    )
            );
        }

        return facts;
    }
}