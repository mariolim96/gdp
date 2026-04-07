package it.csipiemonte.gdp.gdporch.cli;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine.Command;

@TopCommand
@Command(
    name = "gdporch",
    mixinStandardHelpOptions = true,
    subcommands = {
        DatesCommand.class,
        EditionsCommand.class,
        QueueCommand.class,
        DamCommand.class,
        OpsCommand.class
    },
    description = "Orchestratore GdP - CLI Operativa"
)
public class MainCli {
}
