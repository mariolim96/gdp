package it.csipiemonte.gdp.gdporch.service.impl;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneRicercaList;
import it.csipiemonte.gdp.gdporch.dto.TipoEdizione;
import it.csipiemonte.gdp.gdporch.exception.GdpException;
import it.csipiemonte.gdp.gdporch.exception.GdpMessage;
import it.csipiemonte.gdp.gdporch.mapper.AcquisizioneRicercaMapper;
import it.csipiemonte.gdp.gdporch.model.enums.TipoAcquisizione;
import it.csipiemonte.gdp.gdporch.model.projection.AcquisizioneRicercaProjection;
import it.csipiemonte.gdp.gdporch.model.repository.GdpLogRepository;
import it.csipiemonte.gdp.gdporch.service.GdpMonitorAcquisizioniService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
public class GdpMonitorAcquisizioniServiceImpl implements GdpMonitorAcquisizioniService {

    private final GdpLogRepository logRepository;
    private final AcquisizioneRicercaMapper acquisizioneRicercaMapper;

    @Inject
    public GdpMonitorAcquisizioniServiceImpl(
            GdpLogRepository logRepository,
            AcquisizioneRicercaMapper acquisizioneRicercaMapper) {
        this.logRepository = logRepository;
        this.acquisizioneRicercaMapper = acquisizioneRicercaMapper;
    }

    @Override
    public AcquisizioneRicercaList ricercaAcquisizioni(
            String tipoAcquisizione,
            Integer idTestata,
            LocalDate dataA,
            TipoEdizione tipoEdizione,
            LocalDate dataDA) {
        if (idTestata == null) {
            throw new GdpException(GdpMessage.F15_ID_TESTATA_REQUIRED);
        }
        if (dataA == null) {
            throw new GdpException(GdpMessage.F15_DATA_A_REQUIRED);
        }
        if (tipoEdizione == null) {
            throw new GdpException(GdpMessage.F15_TIPO_EDIZIONE_REQUIRED);
        }

        LocalDate effectiveStartDate = dataDA != null ? dataDA : LocalDate.of(1900, 1, 1);
        if (effectiveStartDate.isAfter(dataA)) {
            throw new GdpException(GdpMessage.F15_INVALID_DATE_RANGE);
        }

        TipoAcquisizione acquisitionType;
        try {
            acquisitionType = TipoAcquisizione.fromValue(tipoAcquisizione);
            if (!acquisitionType.name().equalsIgnoreCase(tipoAcquisizione)) {
                throw new IllegalArgumentException("Unsupported acquisition type");
            }
        } catch (Exception exception) {
            throw new GdpException(GdpMessage.F15_INVALID_TIPO_ACQUISIZIONE);
        }

        LocalDateTime startDateTime = effectiveStartDate.atStartOfDay();
        LocalDateTime endDateTimeExclusive = dataA.plusDays(1).atStartOfDay();

        List<AcquisizioneRicercaProjection> projections = logRepository.searchAcquisizioni(
                acquisitionType,
                idTestata,
                startDateTime,
                endDateTimeExclusive,
                it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione.fromValue(tipoEdizione.toString()));

        if (projections.isEmpty()) {
            throw new GdpException(GdpMessage.F15_NO_RESULTS);
        }

        AcquisizioneRicercaList response = new AcquisizioneRicercaList();
        response.setAcquisizioni(projections.stream().map(acquisizioneRicercaMapper::toDto).toList());
        return response;
    }
}