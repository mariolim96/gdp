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

    // =========================
    // CASO A (mensile)
    // =========================
    private List<LocalDate> calcolaCasoA(GdpPeriodicita p,
                                         LocalDate start,
                                         LocalDate end) {

        Log.info("START calcolaCasoA");

        List<LocalDate> result = new ArrayList<>();
        String[] parti = p.getGgPeriodicita().split(";");

        LocalDate current = LocalDate.of(start.getYear(), 1, 1);

        while (!current.isAfter(end)) {

            for (String part : parti) {
                part = part.trim();
                result.addAll(calcolaPatternMensile(part, current, start, end));
            }

            current = incrementaMese(current, p.getMensilita());
        }

        Log.info("END calcolaCasoA");
        return result;
    }

    private List<LocalDate> calcolaPatternMensile(String part,
                                                  LocalDate mese,
                                                  LocalDate start,
                                                  LocalDate end) {

        List<LocalDate> result = new ArrayList<>();

        // Gnn
        if (part.matches("G\\d{2}")) {
            int giorno = Integer.parseInt(part.substring(1));
            if (giorno == 0) giorno = 1;

            LocalDate date = LocalDate.of(
                    mese.getYear(),
                    mese.getMonth(),
                    Math.min(giorno, mese.lengthOfMonth())
            );

            if (isNelRange(date, start, end)) result.add(date);
            return result;
        }

        // GnSm
        if (part.matches("G\\dS\\d")) {
            int ordinale = Integer.parseInt(part.substring(1, 2));
            int sm = Integer.parseInt(part.substring(3));

            // 🔥 GxS0 → settimana completa
            if (sm == 0) {
                return calcolaSettimanaCompleta(ordinale, mese, start, end);
            }

            DayOfWeek dow = convertDay(sm);

            LocalDate date = mese.with(
                    TemporalAdjusters.dayOfWeekInMonth(ordinale, dow)
            );

            if (isNelRange(date, start, end)) result.add(date);
            return result;
        }

        throw new IllegalArgumentException("Formato non valido: " + part);
    }

    private LocalDate incrementaMese(LocalDate current, Double mensilita) {
        if (mensilita >= 1) {
            return current.plusMonths(mensilita.longValue());
        }
        // gestione 0.5 → non salto mesi
        return current.plusMonths(1);
    }

    private List<LocalDate> calcolaSettimanaCompleta(int settimana,
                                                     LocalDate mese,
                                                     LocalDate start,
                                                     LocalDate end) {

        List<LocalDate> result = new ArrayList<>();

        LocalDate first = mese.withDayOfMonth(1);
        LocalDate startWeek = first.plusWeeks(settimana - 1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

        for (int i = 0; i < 7; i++) {
            LocalDate d = startWeek.plusDays(i);
            if (d.getMonth() == mese.getMonth() && isNelRange(d, start, end)) {
                result.add(d);
            }
        }

        return result;
    }

    // =========================
    // CASO B (settimanale)
    // =========================
    private List<LocalDate> calcolaCasoB(GdpPeriodicita p,
                                         LocalDate start,
                                         LocalDate end) {

        Log.info("START calcolaCasoB");

        List<LocalDate> result = new ArrayList<>();
        String[] parti = p.getGgPeriodicita().split(";");

        LocalDate startYear = LocalDate.of(start.getYear(), 1, 1);

        for (String part : parti) {
            part = part.trim();

            String[] split = part.split("WS");
            if (split.length != 2) continue;

            int nW = Integer.parseInt(split[0]);
            int sm = Integer.parseInt(split[1]);

            // 🔥 S0 → quotidiano a blocchi settimanali
            if (sm == 0) {
                result.addAll(calcolaQuotidianoSettimanale(startYear, start, end, nW));
                continue;
            }

            DayOfWeek dow = convertDay(sm);
            LocalDate current = startYear.with(TemporalAdjusters.nextOrSame(dow));

            while (!current.isAfter(end)) {
                if (isNelRange(current, start, end)) {
                    long weeks = ChronoUnit.WEEKS.between(startYear, current);
                    if (weeks % nW == 0) {
                        result.add(current);
                    }
                }
                current = current.plusWeeks(1);
            }
        }

        Log.info("END calcolaCasoB");
        return result;
    }

    private List<LocalDate> calcolaQuotidianoSettimanale(LocalDate startYear,
                                                         LocalDate start,
                                                         LocalDate end,
                                                         int stepWeeks) {

        List<LocalDate> result = new ArrayList<>();
        LocalDate current = startYear;

        while (!current.isAfter(end)) {

            long weeks = ChronoUnit.WEEKS.between(startYear, current);

            if (weeks % stepWeeks == 0 && isNelRange(current, start, end)) {
                result.add(current);
            }

            current = current.plusDays(1);
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