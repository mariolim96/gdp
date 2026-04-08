package it.csipiemonte.gdp.gdporch.mapper;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpUtenteSftp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

@Mapper(componentModel = "jakarta")
public interface AcquisizioneMapper {

    @Mapping(target = "id", source = "dto.idLog")
    @Mapping(target = "fkGdpTestata", source = "dto.idTestata")
    @Mapping(target = "tipoAcquisizione", source = "dto.tipoAcquisizione")
    @Mapping(target = "dataAcquisizione", source = "dto.dataAcquisizione")
    @Mapping(target = "totaleFileAcquisiti", source = "dto.nroTotFile")
    @Mapping(target = "esito", source = "dto.esito")
    @Mapping(target = "fkGdpUtenteFtp", source = "utente.id")
    GdpLog toEntity(AcquisizioneDetail dto, GdpUtenteSftp utente);

    // Converte l'Enum del DTO nel valore String per l'Entity
    default String map(AcquisizioneDetail.TipoAcquisizioneEnum value) {
        return value != null ? value.value() : null;
    }

    default LocalDateTime map(java.util.Date value) {
        return value != null ? LocalDateTime.ofInstant(value.toInstant(), java.time.ZoneId.systemDefault()) : null;
    }
}