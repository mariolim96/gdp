package it.csipiemonte.gdp.gdporch.cli;

import it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService;
import it.csipiemonte.gdp.gdporch.service.HealthCheckService;
import it.csipiemonte.gdp.gdporch.service.PollingManager;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.List;

@Command(name = "ops", description = "Operazioni di manutenzione e diagnostica")
public class OpsCommand implements Runnable {

    @Option(names = {"--polling-enable"}, description = "Abilita lo scanning automatico SFTP")
    boolean pollingEnable;

    @Option(names = {"--polling-disable"}, description = "Disabilita lo scanning automatico SFTP")
    boolean pollingDisable;

    @Option(names = {"--cleanup"}, description = "Esegue la pulizia dei file temporanei (F19 stub)")
    boolean cleanup;

    @Option(names = {"--health-db"}, description = "Esegue diagnostica Database")
    boolean healthDb;

    @Option(names = {"--health-sftp"}, description = "Esegue diagnostica SFTP (con test scrittura)")
    boolean healthSftp;

    @Inject
    PollingManager pollingManager;

    @Inject
    DamTrasmissioneService damService;

    @Inject
    HealthCheckService healthService;

    @Override
    public void run() {
        if (pollingEnable) {
            pollingManager.setEnabled(true);
            System.out.println("Polling abilitato.");
        } else if (pollingDisable) {
            pollingManager.setEnabled(false);
            System.out.println("Polling disabilitato.");
        } else if (cleanup) {
            System.out.println("Avvio cleanup (STUB)...");
            damService.cleanup();
            System.out.println("Operazione completata.");
        } else if (healthDb) {
            System.out.println("--- Diagnostica DATABASE ---");
            List<String> results = healthService.checkDatabase();
            results.forEach(System.out::println);
        } else if (healthSftp) {
            System.out.println("--- Diagnostica SFTP ---");
            List<String> results = healthService.checkSftp();
            results.forEach(System.out::println);
        } else {
            System.out.println("Stato attuale polling: " + pollingManager.getStatus());
            System.out.println("Usa --help per le opzioni disponibili.");
        }
    }
}
