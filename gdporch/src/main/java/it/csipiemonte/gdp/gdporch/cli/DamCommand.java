package it.csipiemonte.gdp.gdporch.cli;

import it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "dam", description = "Interfaccia DAM LIBRA (F20)")
public class DamCommand implements Runnable {

    @Option(names = {"--status"}, description = "Recupera lo stato di un Job DAM")
    String jobId;

    @Inject
    DamTrasmissioneService damService;

    @Override
    public void run() {
        if (jobId != null) {
            System.out.printf("Controllo stato per Job %s...%n", jobId);
            String status = damService.getJobStatus(jobId);
            System.out.printf("Stato attuale: %s%n", status);
        } else {
            System.out.println("Specifca un Job ID (es. --status ID)");
        }
    }
}
