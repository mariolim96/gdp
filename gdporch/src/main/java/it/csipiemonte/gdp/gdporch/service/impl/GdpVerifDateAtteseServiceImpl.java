package it.csipiemonte.gdp.gdporch.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.DateAtteseList;
import it.csipiemonte.gdp.gdporch.dto.DateAttesePerTestata;
import it.csipiemonte.gdp.gdporch.dto.DataAttesa;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.gdporch.service.GdpVerifDateAtteseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GdpVerifDateAtteseServiceImpl implements GdpVerifDateAtteseService {

    @Inject
    GdpTestataRepository gdpTestataRepository;

    @Override
    public DateAtteseList execute(DateRangeRequest request, Integer idTestata) {

        if (request == null || request.getDataInizio() == null || request.getDataFine() == null) {
            throw new IllegalArgumentException("Missing required parameters");
        }

        if (request.getDataInizio().isAfter(request.getDataFine())) {
            throw new IllegalArgumentException("dataInizio > dataFine");
        }

        List<Object[]> rows = gdpTestataRepository.findDateAttese(
                request.getDataInizio(),
                request.getDataFine(),
                idTestata);

        DateAtteseList response = new DateAtteseList();
        if (rows == null || rows.isEmpty()) {
            return response;
        }

        Map<Integer, DateAttesePerTestata> grouped = new LinkedHashMap<>();
        for (Object[] row : rows) {
            Integer testataId = (Integer) row[0];
            String cartellaTestata = (String) row[1];
            LocalDate dataAttesa = (LocalDate) row[2];
            Boolean sospesa = (Boolean) row[3];

            DateAttesePerTestata item = grouped.computeIfAbsent(testataId, id -> new DateAttesePerTestata()
                    .idTestata(id)
                    .cartellaTestata(cartellaTestata)
                    .dateAttese(new ArrayList<>()));

            item.addDateAtteseItem(new DataAttesa().data(dataAttesa).sospesa(sospesa));
            item.nroEdizioniAttese(item.getDateAttese().size());
        }

        response.setTestate(new ArrayList<>(grouped.values()));
        return response;
    }
}
