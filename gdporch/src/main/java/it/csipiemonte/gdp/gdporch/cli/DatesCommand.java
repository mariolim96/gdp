package it.csipiemonte.gdp.gdporch.cli;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.service.ConfigDTEdizioneAttesaService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.LocalDate;

@Command(name = "dates", description = "Gestione date attese (F01)")
public class DatesCommand implements Runnable {

    @Option(names = {"--generate"}, description = "Genera date attese per un periodo")
    boolean generate;

    @Option(names = {"--dataInizio"}, description = "Data inizio (yyyy-MM-dd)", required = true)
    String dataInizio;

    @Option(names = {"--dataFine"}, description = "Data fine (yyyy-MM-dd)", required = true)
    String dataFine;

    @Option(names = {"--idTestata"}, description = "ID Testata (opzionale)")
    Integer idTestata;

    @Inject
    ConfigDTEdizioneAttesaService configService;

    @Override
    public void run() {
        if (generate) {
            System.out.println("Avvio generazione date...");
            DateRangeRequest req = new DateRangeRequest();
            req.setDataInizio(LocalDate.parse(dataInizio));
            req.setDataFine(LocalDate.parse(dataFine));
            req.setIdTestata(idTestata);
            
            var result = configService.calcoloUscite(req);
            System.out.printf("Generazione completata: %d record creati.%n", result.size());
        } else {
            System.out.println("Specifca un'operazione (es. --generate)");
        }
    }
}
