package it.csipiemonte.gdp.gdporch.service;

import io.quarkus.logging.Log;
import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.ResponseDTO;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.repository.GdpDataUscitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpPeriodicitaRepository;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@Transactional
public class UsciteManager {

    @Inject
    GdpDataUscitaService dataUscitaService;

    @Inject
    GdpDataUscitaRepository dataUscitaRepo;

    @Inject
    GdpPeriodicitaRepository periodicitaRepo;

    @Inject
    GdpTestataRepository testataRepo;

    public ResponseDTO calcoloUscite(DateRangeRequest dateRangeRequest) {
        ResponseDTO responseDTO = new ResponseDTO();
        List<GdpTestata> testate = (dateRangeRequest.getIdTestata()!=null)
                ?testataRepo.findByIdOptional(dateRangeRequest.getIdTestata()).map(List::of).orElse(List.of())
                :testataRepo.findActiveSenders();

        if(testate.isEmpty()) {
            Log.info("MSG00001 Il servizio non ha trovato occorrenze per i parametri di input");
            responseDTO.errori.add("MSG00001 Il servizio non ha trovato occorrenze per i parametri di input");
            return responseDTO;
        }

        for(GdpTestata gdpTestata : testate) {
            List<GdpPeriodicita> periodicitaList = periodicitaRepo.findByTestata(gdpTestata.getId());
            if(periodicitaList.isEmpty()) {
                Log.info("Nessuna periodicita per la testata "+gdpTestata.getId());
                continue;
            }
            for(GdpPeriodicita periodicita : periodicitaList) {
                if(!valida(periodicita,gdpTestata,responseDTO))continue;

                try{
                    List<LocalDate> dateAttese = (periodicita.mensilita>0)
                            ? dataUscitaService.calcoloCasoA(periodicita,dateRangeRequest.getDataInizio(),dateRangeRequest.getDataFine())
                            : dataUscitaService.calcolaCasoB(periodicita,dateRangeRequest.getDataInizio(),dateRangeRequest.getDataFine());

                    List<GdpDataUscita> salvate = salvaUscite(periodicita, dateAttese, dateRangeRequest.getDataInizio(), dateRangeRequest.getDataFine());
                    responseDTO.uscite.addAll(salvate);
                } catch (Exception e) {
                    responseDTO.errori.add("Errore calcolo testata " + gdpTestata.getId() + ": " + e.getMessage());
                }
            }

        }
        return responseDTO;
    }

    private boolean valida(GdpPeriodicita p, GdpTestata t, ResponseDTO responseDTO) {
        boolean ok = true;
        if(p.mensilita== null){
            responseDTO.errori.add("MSG002,La testata attiva %s non ha MENSILITA definita"+t.getId());
            ok = false;
        }
        if(p.ggPeriodicita== null || p.ggPeriodicita.isBlank()){
            responseDTO.errori.add("MSG00003 La testata attiva %s non ha GG_PERIODICITA definita"+t.getId());
            ok = false;
        }
        return ok;
    }

    private List<GdpDataUscita> salvaUscite(GdpPeriodicita p,List<LocalDate> dateAttese, LocalDate dataInizio, LocalDate dataFine){
        List<GdpDataUscita> salvate = new ArrayList<>();
        for(LocalDate date : dateAttese){
            // Controlla se nel range di date esiste già un record con la data d
            // Se sì, salta questa iterazione per non creare duplicati
            if (dataUscitaRepo.findByPeriodicitaInRange(p.id, dataInizio, dataFine).stream()
                    .anyMatch(u -> u.dataAttesa.equals(date))) continue;

            GdpDataUscita u = new GdpDataUscita();
            u.fkGdpPeriodicita=p.id;
            u.dataInizio=dataInizio;
            u.dataFine=dataFine;
            u.dataAttesa=date;
            u.sospesa=false;

            dataUscitaRepo.persist(u);
            salvate.add(u);
        }
        return  salvate;
    }

}
