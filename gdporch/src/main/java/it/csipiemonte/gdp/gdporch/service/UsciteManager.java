package it.csipiemonte.gdp.gdporch.service;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.ResponseDTO;
import it.csipiemonte.gdp.gdporch.model.entity.*;
import it.csipiemonte.gdp.gdporch.model.repository.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class UsciteManager {

    @Inject
    GdpDataUscitaService service;

    @Inject
    GdpDataUscitaRepository repo;

    @Inject
    GdpPeriodicitaRepository periodicitaRepo;

    @Inject
    GdpTestataRepository testataRepo;

    @Transactional
    public ResponseDTO calcoloUscite(DateRangeRequest req) {

        Log.info("START calcoloUscite");

        ResponseDTO response = new ResponseDTO();

        if (req.getDataInizio().isAfter(req.getDataFine())) {
            throw new IllegalArgumentException("dataInizio > dataFine");
        }

        List<GdpTestata> testate = (req.getIdTestata() != null)
                ? testataRepo.findByIdOptional(req.getIdTestata()).map(List::of).orElse(List.of())
                : testataRepo.findActiveSenders();

        if (testate.isEmpty()) {
            response.errori.add("Nessuna testata trovata");
            return response;
        }

        for (GdpTestata t : testate) {

            Log.info("Processing testata " + t.getId());

            List<GdpPeriodicita> periodicitaList = periodicitaRepo.findByTestata(t.getId());

            for (GdpPeriodicita p : periodicitaList) {

                if (!valida(p, t, response)) continue;

                try {
                    List<LocalDate> date =
                            service.calcolaDateUscite(p, req.getDataInizio(), req.getDataFine());

                    response.uscite.addAll(
                            salvaBatch(p, t, date, req.getDataInizio(), req.getDataFine())
                    );

                } catch (Exception e) {
                    response.errori.add("Errore testata " + t.getId() + ": " + e.getMessage());
                }
            }
        }

        Log.info("END calcoloUscite");
        return response;
    }

    private boolean valida(GdpPeriodicita p, GdpTestata t, ResponseDTO res) {

        boolean ok = true;

        if (p.getMensilita() == null) {
            res.errori.add(String.format("Testata %s senza MENSILITA", t.getId()));
            ok = false;
        }

        if (p.getGgPeriodicita() == null || p.getGgPeriodicita().isBlank()) {
            res.errori.add(String.format("Testata %s senza GG_PERIODICITA", t.getId()));
            ok = false;
        }

        return ok;
    }

    private List<GdpDataUscita> salvaBatch(GdpPeriodicita p,
                                           GdpTestata t,
                                           List<LocalDate> date,
                                           LocalDate start,
                                           LocalDate end) {

        List<GdpDataUscita> salvate = new ArrayList<>();

        List<GdpDataUscita> esistenti =
                repo.findByPeriodicitaInRange(p.id, start, end);

        Set<LocalDate> giaPresenti = esistenti.stream()
                .map(u -> u.dataAttesa)
                .collect(Collectors.toSet());

        for (LocalDate d : date) {

            if (giaPresenti.contains(d)) continue;

            GdpDataUscita u = new GdpDataUscita();
            u.fkGdpPeriodicita = p.id;
            u.dataAttesa = d;
            u.dataInizio = start;
            u.dataFine = end;
            u.sospesa = false;

            repo.persist(u);
            salvate.add(u);
        }

        return salvate;
    }
}