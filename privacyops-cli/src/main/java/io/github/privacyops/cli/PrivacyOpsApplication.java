package io.github.privacyops.cli;

import picocli.CommandLine;

public final class PrivacyOpsApplication {

    private PrivacyOpsApplication() {
    }

    public static void main(String[] args) {

        int exitCode =
                new CommandLine(
                        new PrivacyOpsCommand()
                ).execute(args);

        System.exit(exitCode);
    }
}