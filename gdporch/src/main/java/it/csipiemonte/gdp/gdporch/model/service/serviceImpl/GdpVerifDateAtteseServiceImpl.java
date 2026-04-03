package it.csipiemonte.gdp.gdporch.model.service.serviceImpl;

import java.time.LocalDate;
import java.util.List;

import it.csipiemonte.gdp.gdporch.model.dto.GdpVerifDateAtteseRequest;
import it.csipiemonte.gdp.gdporch.model.dto.GdpVerifDateAtteseResponse;
import it.csipiemonte.gdp.gdporch.model.repository.GdpTestataRepository;
import it.csipiemonte.gdp.gdporch.model.service.GdpVerifDateAtteseService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GdpVerifDateAtteseServiceImpl implements GdpVerifDateAtteseService {

    @Inject
    GdpTestataRepository gdpTestataRepository;

    @Override
    @Transactional
    public GdpVerifDateAtteseResponse execute(GdpVerifDateAtteseRequest request) {

        // Validazione base
        if (request.dataInizio.isAfter(request.dataFine)) {
            throw new IllegalArgumentException("dataInizio > dataFine");
        }

        List<Object[]> rows = gdpTestataRepository.findDateAttese(
                request.dataInizio,
                request.dataFine,
                request.idTestata);

        if (rows.isEmpty()) {
            return new GdpVerifDateAtteseResponse("MSG00001", "Nessun risultato trovato", List.of());
        }

        List<GdpVerifDateAtteseResponse.GdpResultItem> risultati = rows.stream()
                .map(r -> new GdpVerifDateAtteseResponse.GdpResultItem(
                        (Integer) r[0],
                        (String) r[1],
                        (LocalDate) r[2],
                        (Boolean) r[3]))
                .toList();

        return new GdpVerifDateAtteseResponse("MSG00009", "Elaborazione completata correttamente", risultati);
    }
}
