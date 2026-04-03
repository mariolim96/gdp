package it.csipiemonte.gdp.gdporch.cli;

import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.service.GdpDataUscitaService;
import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.LocalDate;
import java.util.List;

@Command(name = "test-uscite-entity", mixinStandardHelpOptions = true,
        description = "Test dei calcoli delle uscite direttamente su entity GdpPeriodicita")
public class TestUsciteCommand implements Runnable {

    @Inject
    GdpDataUscitaService dataUscitaService;

    @Option(names = {"-s", "--start"}, description = "Data inizio (yyyy-MM-dd)", required = true)
    String start;

    @Option(names = {"-e", "--end"}, description = "Data fine (yyyy-MM-dd)", required = true)
    String end;

    @Option(names = {"-g", "--ggPeriodicita"}, description = "GG_PERIODICITA, es: G1S0, 2WS0", required = true)
    String ggPeriodicita;

    @Option(names = {"-m", "--mensilita"}, description = "Mensilità, es: 1, 0.5, 0.0", required = true)
    Double mensilita;

    @Option(names = {"-t", "--testata"}, description = "ID Testata (facoltativo)")
    Integer idTestata;

    @Override
    public void run() {
        LocalDate dataInizio = LocalDate.parse(start);
        LocalDate dataFine = LocalDate.parse(end);

        // Creo direttamente l'entity per il test
        GdpPeriodicita periodicita = new GdpPeriodicita();
        periodicita.setFkGdpTestata(idTestata != null ? idTestata : 0); // 0 se testata fittizia
        periodicita.setGgPeriodicita(ggPeriodicita);
        periodicita.setMensilita(mensilita);

        System.out.println("---- CALCOLO USCITE PER ENTITY ----");
        List<LocalDate> dateAttese = dataUscitaService.calcolaDateUscite(periodicita, dataInizio, dataFine);

        dateAttese.forEach(d -> System.out.println("Data Attesa: " + d));

        if (dateAttese.isEmpty()) {
            System.out.println("Nessuna uscita calcolata per i parametri forniti.");
        }
    }
}