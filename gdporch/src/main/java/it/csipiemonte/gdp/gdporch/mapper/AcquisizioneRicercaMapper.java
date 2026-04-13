package it.csipiemonte.gdp.gdporch.mapper;

import it.csipiemonte.gdp.gdporch.dto.AcquisizioneRicercaSummary;
import it.csipiemonte.gdp.gdporch.dto.TipoEdizione;
import it.csipiemonte.gdp.gdporch.model.projection.AcquisizioneRicercaProjection;
import java.time.ZoneId;
import java.util.Date;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "jakarta")
public interface AcquisizioneRicercaMapper {

    @Mapping(target = "dataAcquisizione", expression = "java(toDate(projection.dataAcquisizione()))")
    @Mapping(target = "tipoEdizione", expression = "java(toDtoTipoEdizione(projection.tipoEdizione()))")
    AcquisizioneRicercaSummary toDto(AcquisizioneRicercaProjection projection);

    default Date toDate(java.time.LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }

    default TipoEdizione toDtoTipoEdizione(it.csipiemonte.gdp.gdporch.model.enums.TipoEdizione value) {
        if (value == null) {
            return null;
        }
        return TipoEdizione.fromValue(value.name());
    }
}