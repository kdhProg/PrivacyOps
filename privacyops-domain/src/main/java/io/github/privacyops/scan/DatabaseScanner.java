package io.github.privacyops.scan;

import io.github.privacyops.fact.Fact;

import java.sql.Connection;
import java.util.List;

public interface DatabaseScanner {

    List<Fact> scan(
            Connection connection,
            String schema
    );
}