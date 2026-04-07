package it.csipiemonte.gdp.gdporch.service;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@ApplicationScoped
public class GdpDataUscitaService {

    public List<LocalDate> calcolaDateUscite(GdpPeriodicita periodicita,
                                             LocalDate dataInizio,
                                             LocalDate dataFine) {
        Log.info("START calcolaDateUscite periodicita=" + periodicita.id);

        if (periodicita.getMensilita() == null ||
                periodicita.getGgPeriodicita() == null ||
                periodicita.getGgPeriodicita().isBlank()) {
            throw new IllegalArgumentException("Periodicità non valida");
        }
        List<LocalDate> result = (periodicita.getMensilita() > 0)
                ? calcolaCasoA(periodicita, dataInizio, dataFine)
                : calcolaCasoB(periodicita, dataInizio, dataFine);
        List<LocalDate> clean = result.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();
        Log.info("END calcolaDateUscite -> trovate " + clean.size() + " date");
        return clean;
    }

    // CASO A — mensilita > 0
    // La testata esce un certo numero di volte al mese.
    // GG_PERIODICITA può contenere più pattern separati da ";"
    // Forme supportate:
    //   Gnn    → giorno fisso del mese (es: G01, G15)
    //   GnSm   → n-esimo giorno della settimana del mese (es: G1S6 = primo sabato)
    //   GnS0   → ESTENSIONE: n-esima settimana completa del mese (tutti i giorni lun-dom)
    private List<LocalDate> calcolaCasoA(GdpPeriodicita p,
                                         LocalDate start,
                                         LocalDate end) {
        Log.info("START calcolaCasoA");
        List<LocalDate> result = new ArrayList<>();
        String[] parti = p.getGgPeriodicita().split(";");
        // Il calcolo parte sempre dal 1° gennaio dell'anno di inizio
        // come da specifica: "trovo il primo giorno di uscita a partire dal 1° gennaio"
        LocalDate current = LocalDate.of(start.getYear(), 1, 1);
        while (!current.isAfter(end)) {
            for (String part : parti) {
                result.addAll(calcolaPatternMensile(part.trim(), current, start, end));
            }
            current = incrementaMese(current, p.getMensilita());
        }
        Log.info("END calcolaCasoA");
        return result;
    }

    /**
     * Calcola la/le date prodotte da un singolo pattern nel mese indicato.
     * Pattern Gnn/GnSm
     */
    private List<LocalDate> calcolaPatternMensile(String part,
                                                  LocalDate mese,
                                                  LocalDate start,
                                                  LocalDate end) {
        List<LocalDate> result = new ArrayList<>();

        // --- Forma Gnn: giorno fisso del mese ---
        if (part.matches("G\\d{2}")) {
            int giorno = Integer.parseInt(part.substring(1));
            if (giorno == 0) giorno = 1; // G00 → default primo del mese

            LocalDate date = LocalDate.of(
                    mese.getYear(),
                    mese.getMonth(),
                    Math.min(giorno, mese.lengthOfMonth()) // gestisce mesi corti
            );

            if (isNelRange(date, start, end)) result.add(date);
            return result;
        }

        // --- Forma GnSm: n-esimo giorno della settimana del mese ---
        if (part.matches("G\\dS\\d")) {
            int ordinale = Integer.parseInt(part.substring(1, 2)); // es: 1 in G1S6
            int sm       = Integer.parseInt(part.substring(3));    // es: 6 in G1S6

            // CASO SPECIALE GnS0: n-esima settimana completa del mese
            // Non previsto dalla specifica ufficiale ma gestito per estensione:
            // restituisce tutti i giorni (lun-dom) della n-esima settimana
            // che hanno almeno un giorno nel mese corrente.
            if (sm == 0) {
                return calcolaSettimanaCompleta(ordinale, mese, start, end);
            }
            // Caso normale GnSm: n-esimo giorno della settimana
            DayOfWeek dow = convertDay(sm);
            LocalDate date = mese.with(TemporalAdjusters.dayOfWeekInMonth(ordinale, dow));
            if (isNelRange(date, start, end)) result.add(date);
            return result;
        }

        throw new IllegalArgumentException("Formato GG_PERIODICITA non valido per caso A: " + part);
    }
    /**
     * ESTENSIONE GnS0 — Restituisce tutti i giorni della n-esima settimana del mese.
     * Usato solo nel CASO A (mensile). La settimana è intesa come blocco lun-dom
     * del calendario, anche se inizia nel mese precedente.
     * La "n-esima settimana" è calcolata come:
     *   - si trova il lunedì precedente o uguale al 1° del mese
     *   - si avanza di (n-1) settimane
     *   - si includono solo i giorni che appartengono al mese corrente
     */
    private List<LocalDate> calcolaSettimanaCompleta(int settimana,
                                                     LocalDate mese,
                                                     LocalDate start,
                                                     LocalDate end) {
        List<LocalDate> result = new ArrayList<>();

        // Lunedì della prima settimana che contiene giorni del mese
        LocalDate primoDelMese = mese.withDayOfMonth(1);
        LocalDate lunediPrimaSettimana = primoDelMese
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        // Lunedì della n-esima settimana (settimana=1 → nessun avanzamento)
        LocalDate startWeek = lunediPrimaSettimana.plusWeeks(settimana - 1);

        for (int i = 0; i < 7; i++) {
            LocalDate d = startWeek.plusDays(i);
            // Includo solo giorni che appartengono al mese di riferimento
            if (d.getMonth() == mese.getMonth() && isNelRange(d, start, end)) {
                result.add(d);
            }
        }

        return result;
    }

     // Incrementa il mese corrente in base alla mensilità.
    private LocalDate incrementaMese(LocalDate current, Integer mensilita) {
        long mesi = Math.max(1, Math.round(mensilita));
        return current.plusMonths(mesi);
    }

    // CASO B — mensilita = 0
    // La testata esce un certo numero di volte a settimana.
    // GG_PERIODICITA può contenere più pattern separati da ";"
    // Forma: nWSm
    //   nW → intervallo in settimane tra le uscite
    //   Sm → giorno della settimana (S0 = quotidiano, S1=lun ... S7=dom)
    private List<LocalDate> calcolaCasoB(GdpPeriodicita p,
                                         LocalDate start,
                                         LocalDate end) {
        Log.info("START calcolaCasoB");
        List<LocalDate> result = new ArrayList<>();
        String[] parti = p.getGgPeriodicita().split(";");
        // Il calcolo parte sempre dal 1° gennaio dell'anno di inizio
        LocalDate startYear = LocalDate.of(start.getYear(), 1, 1);

        for (String part : parti) {
            part = part.trim();
            if (!part.matches("\\d+WS\\d")) {
                throw new IllegalArgumentException("Formato GG_PERIODICITA non valido per caso B: " + part);
            }
            String[] split = part.split("WS");
            int nW = Integer.parseInt(split[0]); // intervallo in settimane
            int sm = Integer.parseInt(split[1]);  // giorno della settimana
            if (sm == 0) {
                // CASO SPECIALE nWS0:
                //   1WS0 → tutti i giorni (quotidiano)
                //   2WS0 → ESTENSIONE: tutti i giorni di ogni n-esima settimana
                //           (settimana completa a intervalli di nW settimane)
                result.addAll(calcolaQuotidianoOSettimanaCompleta(startYear, start, end, nW));
            } else {
                // Caso normale nWSm: ogni nW settimane, nel giorno sm
                result.addAll(calcolaSettimanale(startYear, start, end, nW, convertDay(sm)));
            }
        }
        Log.info("END calcolaCasoB");
        return result;
    }

    /**
     * Gestisce il caso nWS0:
     * - 1WS0: tutti i giorni dell'anno nel range (quotidiano)
     * - 2WS0: tutti i giorni di ogni seconda settimana (settimana completa alternata)
     * - nWS0: tutti i giorni di ogni n-esima settimana a partire dal 1° gennaio
     * La "settimana" è intesa come blocco lun-dom a partire dal 1° gennaio.
     */
    private List<LocalDate> calcolaQuotidianoOSettimanaCompleta(LocalDate startYear,
                                                                LocalDate start,
                                                                LocalDate end,
                                                                int stepWeeks) {

        List<LocalDate> result = new ArrayList<>();
        if (stepWeeks == 1) {
            // 1WS0 → tutti i giorni nel range, senza logica settimanale
            LocalDate current = start;
            while (!current.isAfter(end)) {
                result.add(current);
                current = current.plusDays(1);
            }
            return result;
        }

// Prima "settimana": dal 1° gennaio alla domenica successiva
        LocalDate fineSettimana1 = startYear.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        int settimana = 1;
// Processo la settimana 1 (01/gen → prima domenica)
        if (settimana % stepWeeks == 0) {
            LocalDate d = startYear;
            while (!d.isAfter(fineSettimana1)) {
                if (isNelRange(d, start, end)) result.add(d);
                d = d.plusDays(1);
            }
        }
// Dalla settimana 2 in poi: blocchi lun-dom standard
        LocalDate current = fineSettimana1.plusDays(1); // primo lunedì
        settimana++;
        while (!current.isAfter(end)) {
            if (settimana % stepWeeks == 0) {
                for (int i = 0; i < 7; i++) {
                    LocalDate d = current.plusDays(i);
                    if (isNelRange(d, start, end)) result.add(d);
                }
            }
            current = current.plusWeeks(1);
            settimana++;
        }
        return result;
    }
    /**
     * Calcola le uscite per un giorno della settimana specifico (dow),
     * ogni nW settimane, a partire dalla prima occorrenza dal 1° gennaio.
     */
    private List<LocalDate> calcolaSettimanale(LocalDate startYear,
                                               LocalDate start,
                                               LocalDate end,
                                               int nW,
                                               DayOfWeek dow) {
        List<LocalDate> result = new ArrayList<>();
        // Prima occorrenza del giorno richiesto dal 1° gennaio
        LocalDate current = startYear.with(TemporalAdjusters.nextOrSame(dow));
        while (!current.isAfter(end)) {
            long weeks = ChronoUnit.WEEKS.between(startYear, current);
            if (weeks % nW == 0 && isNelRange(current, start, end)) {
                result.add(current);
            }
            current = current.plusWeeks(1);
        }
        return result;
    }

    private boolean isNelRange(LocalDate d, LocalDate start, LocalDate end) {
        return d != null && !d.isBefore(start) && !d.isAfter(end);
    }

    private DayOfWeek convertDay(int sm) {
        return switch (sm) {
            case 1 -> DayOfWeek.MONDAY;
            case 2 -> DayOfWeek.TUESDAY;
            case 3 -> DayOfWeek.WEDNESDAY;
            case 4 -> DayOfWeek.THURSDAY;
            case 5 -> DayOfWeek.FRIDAY;
            case 6 -> DayOfWeek.SATURDAY;
            case 7 -> DayOfWeek.SUNDAY;
            default -> throw new IllegalArgumentException("Giorno non valido: " + sm);
        };
    }
}