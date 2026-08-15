package io.github.privacyops.cli;

import picocli.CommandLine.Command;

@Command(
        name = "privacyops",
        description = "Privacy governance analysis for software projects.",
        mixinStandardHelpOptions = true,
        version = "PrivacyOps 0.1.0",
        subcommands = {
                ScanCommand.class
        }
)
public class PrivacyOpsCommand implements Runnable {

    @Override
    public void run() {
        System.out.println(
                "Use 'privacyops --help' to see available commands."
        );
    }
}