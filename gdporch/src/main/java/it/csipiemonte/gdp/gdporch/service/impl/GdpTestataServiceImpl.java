package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.dto.TestataDetail;
import it.csipiemonte.gdp.gdporch.dto.TestataSummary;
import it.csipiemonte.gdp.gdporch.dto.TestataSummaryList;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.gdporch.service.GdpTestataService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class GdpTestataServiceImpl implements GdpTestataService {

    @Inject
    GdpTestataRepository gdpTestataRepository;

    @Override
    public TestataSummaryList elencoTestate(Boolean invioEdizione, String prov, Integer idTestata) {
        int filters = 0;
        if (invioEdizione != null) {
            filters++;
        }
        if (prov != null) {
            filters++;
        }
        if (idTestata != null) {
            filters++;
        }

        if (filters > 1) {
            throw new GdpException(GdpMessage.F16_INVALID_FILTERS);
        }

        List<GdpTestata> testate;
        if (idTestata != null) {
            GdpTestata testata = gdpTestataRepository.findById(idTestata);
            testate = testata != null ? Collections.singletonList(testata) : Collections.emptyList();
        } else if (prov != null) {
            testate = gdpTestataRepository.findByProvincia(prov);
        } else if (invioEdizione != null) {
            testate = gdpTestataRepository.findByInvioEdizione(invioEdizione);
        } else {
            testate = gdpTestataRepository.listAll();
        }

        List<TestataSummary> summaries = testate.stream()
                .map(this::toTestataSummary)
                .collect(Collectors.toList());

        return new TestataSummaryList().testate(new ArrayList<>(summaries));
    }

    @Override
    public TestataDetail getTestataById(Integer idTestata) {
        GdpTestata testata = gdpTestataRepository.findById(idTestata);
        if (testata == null) {
            throw new GdpException(GdpMessage.F17_NOT_FOUND);
        }
        return toTestataDetail(testata);
    }

    private TestataSummary toTestataSummary(GdpTestata testata) {
        return new TestataSummary()
                .idTestata(testata.getId())
                .nomeTestata(testata.getNomeTestata())
                .cartellaTestata(testata.getCartellaTestata())
                .invioEdizione(testata.getInvioEdizione())
                .provincia(testata.getProvincia());
    }

    private TestataDetail toTestataDetail(GdpTestata testata) {
        return new TestataDetail()
                .idTestata(testata.getId())
                .nomeTestata(testata.getNomeTestata())
                .cartellaTestata(testata.getCartellaTestata())
                .stato(testata.getStato())
                .cancellazione(testata.getCancellazione() != null ? testata.getCancellazione().toString() : null)
                .dataStato(testata.getDataStato())
                .codTema(testata.getCodTema() != null ? testata.getCodTema().toString() : null)
                .tema(testata.getTema())
                .descrizione(testata.getDescrizione())
                .mail(testata.getMail())
                .cap(testata.getCap())
                .codEditrice(testata.getSocEditrice())
                .annoFondazione(testata.getAnnoFondazione())
                .periodoDal(testata.getPeriodoFreq())
                .periodoAl(testata.getPeriodoGg())
                .sitoWeb(testata.getWww() != null ? (testata.getWww().startsWith("http") ? java.net.URI.create(testata.getWww()) : java.net.URI.create("http://" + testata.getWww())) : null)
                .entePropietario(testata.getEnteProponente())
                .invioEdizione(testata.getInvioEdizione())
                .comune(testata.getComune())
                .indirizzo(testata.getIndirizzo())
                .latitudine(testata.getLatitudine() != null ? testata.getLatitudine().doubleValue() : null)
                .longitudine(testata.getLongitudine() != null ? testata.getLongitudine().doubleValue() : null)
                .provincia(testata.getProvincia());
    }
}
