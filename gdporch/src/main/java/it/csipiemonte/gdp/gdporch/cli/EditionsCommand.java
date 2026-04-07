package it.csipiemonte.gdp.gdporch.cli;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.service.GdpSospensioneService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.LocalDate;

@Command(name = "editions", description = "Gestione edizioni e sospensioni (F05)")
public class EditionsCommand implements Runnable {

    @Option(names = {"--suspend"}, description = "Sospende le edizioni per un periodo")
    boolean suspend;

    @Option(names = {"--dataInizio"}, description = "Data inizio (yyyy-MM-dd)", required = true)
    String dataInizio;

    @Option(names = {"--dataFine"}, description = "Data fine (yyyy-MM-dd)", required = true)
    String dataFine;

    @Option(names = {"--idTestata"}, description = "ID Testata", required = true)
    Integer idTestata;

    @Inject
    GdpSospensioneService sospensioneService;

    @Override
    public void run() {
        if (suspend) {
            System.out.println("Avvio sospensione edizioni...");
            DateRangeRequest req = new DateRangeRequest();
            req.setDataInizio(LocalDate.parse(dataInizio));
            req.setDataFine(LocalDate.parse(dataFine));
            
            var result = sospensioneService.sospendi(idTestata, req);
            System.out.printf("Sospensione completata: %d giorni sospesi. Messaggio: %s%n", 
                result.getGiorniSospesi(), result.getMessage());
        } else {
            System.out.println("Specifca un'operazione (es. --suspend)");
        }
    }
}
