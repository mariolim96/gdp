package it.csipiemonte.gdp.gdporch.mapper;

import it.csipiemonte.gdp.gdporch.dto.DateRangeRequest;
import it.csipiemonte.gdp.gdporch.dto.SospensioneResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "jakarta")
public interface SospensioneMapper {

    SospensioneMapper INSTANCE = Mappers.getMapper(SospensioneMapper.class);

    // If we needed to map from Entity to DTO, we would add it here.
    // For now, the service creates the DTO directly, but we could use a mapper
    // if we had a domain object in between.
}
