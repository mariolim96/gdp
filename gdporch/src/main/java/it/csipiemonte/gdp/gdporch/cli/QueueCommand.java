package it.csipiemonte.gdp.gdporch.cli;

import it.csipiemonte.gdp.gdporch.service.DamTrasmissioneService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "queue", description = "Gestione coda di caricamento (F21/F10)")
public class QueueCommand implements Runnable {

    @Option(names = {"--retry"}, description = "Ripristina in coda un invio fallito (F21)")
    Integer idLog;

    @Option(names = {"--flush"}, description = "Svuota immediatamente la coda (F10)")
    boolean flush;

    @Inject
    DamTrasmissioneService damService;

    @Override
    public void run() {
        if (idLog != null) {
            System.out.printf("Ripristino invio per idLog %d...%n", idLog);
            damService.retry(idLog);
            System.out.println("Operazione completata.");
        } else if (flush) {
            System.out.println("Avvio flush della coda...");
            damService.flush();
            System.out.println("Operazione completata.");
        } else {
            System.out.println("Specifca un'operazione (es. --retry ID o --flush)");
        }
    }
}
