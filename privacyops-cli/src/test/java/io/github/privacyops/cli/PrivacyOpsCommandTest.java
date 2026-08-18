package io.github.privacyops.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrivacyOpsCommandTest {

    @Test
    void showsHelpSuccessfully() {

        CommandLine commandLine =
                new CommandLine(
                        new PrivacyOpsCommand()
                );

        int exitCode =
                commandLine.execute("--help");

        assertEquals(
                0,
                exitCode
        );
    }

    @Test
    void rejectsIncompleteDatabaseOptions() {

        CommandLine commandLine =
                new CommandLine(
                        new PrivacyOpsCommand()
                );

        int exitCode =
                commandLine.execute(
                        "scan",
                        ".",
                        "--db-url",
                        "jdbc:h2:mem:test"
                );

        assertEquals(
                2,
                exitCode
        );
    }
}