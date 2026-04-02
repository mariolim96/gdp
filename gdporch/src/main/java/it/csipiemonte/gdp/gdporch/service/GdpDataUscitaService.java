package it.csipiemonte.gdp.gdporch.service;


import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class GdpDataUscitaService {

    public List<LocalDate> calcolaDateUscite(GdpPeriodicita periodicita,
                                             LocalDate dataInizio,
                                             LocalDate dataFine) {

        List<LocalDate> result = new ArrayList<>();

        if (periodicita.getMensilita() == null ||
                periodicita.getGgPeriodicita() == null ||
                periodicita.getGgPeriodicita().isBlank()) {

            // gestione errori fatta nel manager
            return result;
        }

        if (periodicita.getMensilita() > 0) {
            result.addAll(calcolaCasoA(periodicita, dataInizio, dataFine));
        } else {
            result.addAll(calcolaCasoB(periodicita, dataInizio, dataFine));
        }

        return result;
    }

    // Caso A: periodicità mensile (mensilita > 0)
    private List<LocalDate> calcolaCasoA(GdpPeriodicita periodicita,
                                         LocalDate dataInizio,
                                         LocalDate dataFine) {

        List<LocalDate> result = new ArrayList<>();
        String[] parti = periodicita.getGgPeriodicita().split(";");
        // si parte dal 1° gennaio dell'anno
        LocalDate currentMonth = LocalDate.of(dataInizio.getYear(), 1, 1);

        while (!currentMonth.isAfter(dataFine)) {
            for (String part : parti) {
                LocalDate uscita = calcolaGiornoMensile(part.trim(), currentMonth);
                if (isNelRange(uscita, dataInizio, dataFine)) {
                    result.add(uscita);
                }
            }
            // incremento mesi in base alla mensilità
            currentMonth = currentMonth.plusMonths(periodicita.getMensilita());
        }

        return result;
    }

    //Gestione Gnn e GnSm
    private LocalDate calcolaGiornoMensile(String part, LocalDate monthReference) {
        // Gnn → giorno del mese
        if (part.matches("G\\d{2}")) {
            int giorno = Integer.parseInt(part.substring(1));

            if (giorno == 0) giorno = 1; // G00 → primo giorno del mese
            return LocalDate.of(
                    monthReference.getYear(),
                    monthReference.getMonth(),
                    Math.min(giorno, monthReference.lengthOfMonth())
            );
        }
        // GnSm → es: G1S6 (primo sabato)
        if (part.matches("G\\dS\\d")) {
            int ordinale = Integer.parseInt(part.substring(1, 2));
            int sm = Integer.parseInt(part.substring(3));

            DayOfWeek dow = convertDay(sm);
            return monthReference.with(
                    TemporalAdjusters.dayOfWeekInMonth(ordinale, dow)
            );
        }
        return null;
    }
    //Caso B: periodicità settimanale (mensilita = 0)
    private List<LocalDate> calcolaCasoB(GdpPeriodicita periodicita,
                                         LocalDate dataInizio,
                                         LocalDate dataFine) {

        List<LocalDate> result = new ArrayList<>();
        String[] parti = periodicita.getGgPeriodicita().split(";");
        for (String part : parti) {
            part = part.trim();

            if (!part.matches("\\d+WS\\d")) continue;
            int nW = Integer.parseInt(part.substring(0, 1)); // intervallo settimane
            int sm = Integer.parseInt(part.substring(3));    // giorno settimana

            //  CASO SPECIALE: QUOTIDIANO (S0)
            if (sm == 0) {
                result.addAll(calcolaQuotidiano(dataInizio, dataFine, nW));
                continue;
            }
            DayOfWeek dow = convertDay(sm);
            // primo giorno valido >= dataInizio
            LocalDate current = dataInizio.with(TemporalAdjusters.nextOrSame(dow));
            while (!current.isAfter(dataFine)) {
                result.add(current);
                current = current.plusWeeks(nW);
            }
        }
        return result;
    }
    //Gestione quotidiano (S0)
    private List<LocalDate> calcolaQuotidiano(LocalDate start,
                                              LocalDate end,
                                              int step) {

        List<LocalDate> result = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            result.add(current);
            current = current.plusDays(step);
        }
        return result;
    }
    //Controlla se una data è nel range richiesto
    private boolean isNelRange(LocalDate date,
                               LocalDate start,
                               LocalDate end) {

        return date != null &&
                !date.isBefore(start) &&
                !date.isAfter(end);
    }

    //Converte Sm (1-7) in DayOfWeek
    private DayOfWeek convertDay(int sm) {
        return switch (sm) {
            case 1 -> DayOfWeek.MONDAY;
            case 2 -> DayOfWeek.TUESDAY;
            case 3 -> DayOfWeek.WEDNESDAY;
            case 4 -> DayOfWeek.THURSDAY;
            case 5 -> DayOfWeek.FRIDAY;
            case 6 -> DayOfWeek.SATURDAY;
            case 7 -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Giorno settimana non valido: " + sm);
        };
    }
}


