package it.csipiemonte.gdp.gdporch.cli;


import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.ResponseDTO;
import it.csipiemonte.gdp.gdporch.model.service.UsciteManager;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.LocalDate;

@Command(name = "test-uscite", mixinStandardHelpOptions = true,
        description = "Test dei calcoli delle uscite (Caso A e Caso B)")
public class TestUsciteCommand implements Runnable {

    @Inject
    UsciteManager manager;

    @Option(names = {"-s", "--start"}, description = "Data inizio (yyyy-MM-dd)", required = true)
    String start;

    @Option(names = {"-e", "--end"}, description = "Data fine (yyyy-MM-dd)", required = true)
    String end;

    @Option(names = {"-t", "--testata"}, description = "ID Testata (facoltativo)")
    Long idTestata;

    @Override
    public void run() {
        LocalDate dataInizio = LocalDate.parse(start);
        LocalDate dataFine = LocalDate.parse(end);

        DateRangeRequest request = new DateRangeRequest();
        request.setDataInizio(dataInizio);
        request.setDataFine(dataFine);
        request.setIdTestata(idTestata);

        ResponseDTO response = manager.calcoloUscite(request);

        System.out.println("---- RISULTATI CALCOLO USCITE ----");
        response.getUscite().forEach(u ->
                System.out.println("Testata: " + u.getFkGdpPeriodicita() +
                        " - Data Attesa: " + u.getDataAttesa())
        );

        if (!response.getErrori().isEmpty()) {
            System.out.println("\n---- ERRORI ----");
            response.getErrori().forEach(System.out::println);
        }
    }
}