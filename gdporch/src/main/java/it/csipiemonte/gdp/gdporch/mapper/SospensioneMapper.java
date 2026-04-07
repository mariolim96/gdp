package it.csipiemonte.gdp.gdporch.mapper;

import it.csipiemonte.gdp.gdporch.dto.SospensioneResponse;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "jakarta")
public interface SospensioneMapper {

    SospensioneMapper INSTANCE = Mappers.getMapper(SospensioneMapper.class);

    @Mapping(target = "giorniSospesi", source = "id")
    @Mapping(target = "message", constant = "MSG00009")
    SospensioneResponse toResponse(GdpDataUscita entity);
}


