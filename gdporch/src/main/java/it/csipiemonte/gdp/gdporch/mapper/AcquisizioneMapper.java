package it.csipiemonte.gdp.gdporch.mapper;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneDetail;
import it.csipiemonte.gdp.gdporch.model.entity.GdpLog;
import it.csipiemonte.gdp.gdporch.model.entity.GdpUtenteSftp;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.LocalDate;

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

    // Se la tua Entity GdpLog usa LocalDate (come visto prima)
    default LocalDate map(OffsetDateTime value) {
        return value != null ? value.toLocalDate() : null;
    }

    // Se invece l'Entity usa LocalDateTime, usa questo:
    /*
    default LocalDateTime map(OffsetDateTime value) {
        return value != null ? value.toLocalDateTime() : null;
    }
    */
}