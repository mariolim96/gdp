package it.csipiemonte.gdp.gdporch.mapper;

import it.csipiemonte.gdp.gdporch.dto.DataAttesa;
import it.csipiemonte.gdp.gdporch.dto.DateAttesePerTestata;
import it.csipiemonte.gdp.gdporch.model.entity.GdpDataUscita;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPeriodicita;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Mapper(componentModel = "cdi")
public interface GdpDataUscitaMapper {

    @Mapping(source = "dataAttesa", target = "data")
    @Mapping(source = "sospesa", target = "sospesa")
    DataAttesa toDataAttesa(GdpDataUscita entity);

    default List<DateAttesePerTestata> toDateAttesePerTestata(List<GdpDataUscita> entities,
                                                              Map<Integer, GdpPeriodicita> periodicitaMap) {

        // Raggruppa le uscite per id testata tramite periodicità
        Map<Long, List<GdpDataUscita>> grouped = entities.stream()
                .collect(Collectors.groupingBy(u ->
                        periodicitaMap.get(u.getFkGdpPeriodicita()).getFkGdpTestata().longValue()
                ));

        return grouped.entrySet().stream()
                .map(entry -> {
                    DateAttesePerTestata dto = new DateAttesePerTestata();
                    dto.setIdTestata(entry.getKey().intValue());
                    dto.setDateAttese(entry.getValue().stream()
                            .map(this::toDataAttesa)
                            .collect(Collectors.toList()));
                    return dto;
                })
                .collect(Collectors.toList());
    }
}