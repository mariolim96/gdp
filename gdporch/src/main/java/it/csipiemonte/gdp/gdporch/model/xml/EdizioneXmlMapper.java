package it.csipiemonte.gdp.gdporch.model.xml;

import it.csipiemonte.gdp.gdporch.model.entity.GdpEdizione;
import it.csipiemonte.gdp.gdporch.model.entity.GdpPagina;
import it.csipiemonte.gdp.gdporch.model.entity.GdpTestata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@Mapper(componentModel = "jakarta")
public interface EdizioneXmlMapper {

    // -------------------------------------------------------------------------
    // TestataMetadata
    // -------------------------------------------------------------------------

    @Mapping(target = "idTestata",       source = "id")
    @Mapping(target = "nomeTestata",     source = "nomeTestata")
    @Mapping(target = "cartella",        source = "cartellaTestata")
    @Mapping(target = "stato",           source = "stato")
    @Mapping(target = "dataStato",       source = "dataStato")
    @Mapping(target = "cancellazione",   source = "cancellazione")
    @Mapping(target = "codTema",         source = "codTema")
    @Mapping(target = "tema",            source = "tema")
    @Mapping(target = "socEditrice",     source = "socEditrice")
    @Mapping(target = "enteProponente",  source = "enteProponente")
    @Mapping(target = "annoFondazione",  source = "annoFondazione")
    @Mapping(target = "periodoFreq",     source = "periodoFreq")
    @Mapping(target = "periodoGg",       source = "periodoGg")
    @Mapping(target = "descrizione",     source = "descrizione")
    @Mapping(target = "www",             source = "www")
    @Mapping(target = "mail",            source = "mail")
    @Mapping(target = "provincia",       source = "provincia")
    @Mapping(target = "comune",          source = "comune")
    @Mapping(target = "indirizzo",       source = "indirizzo")
    @Mapping(target = "cap",             source = "cap")
    @Mapping(target = "longitudine",     source = "longitudine")
    @Mapping(target = "latitudine",      source = "latitudine")
    TestataMetadata toTestataMetadata(GdpTestata entity);

    // -------------------------------------------------------------------------
    // EdizioneMetadata
    // -------------------------------------------------------------------------

    @Mapping(target = "idEdizione",         source = "id")
    @Mapping(target = "fkGdpTestata",       source = "fkGdpTestata")
    @Mapping(target = "dataEdizione",       source = "dataEdizione")
    @Mapping(target = "dataPubblicazione",  source = "dataPubblicazione")
    @Mapping(target = "stato",              source = "stato")
    @Mapping(target = "numeroPagine",       source = "totalePagine")
    EdizioneMetadata toEdizioneMetadata(GdpEdizione entity);

    // -------------------------------------------------------------------------
    // PaginaMetadata
    // -------------------------------------------------------------------------

    @Mapping(target = "idPagina",       source = "id")
    @Mapping(target = "fkGdpTestata",   source = "fkGdpTestata")
    @Mapping(target = "fkGdpEdizione",  source = "fkGdpEdizione")
    @Mapping(target = "numPagina",      source = "numPagina")
    @Mapping(target = "filePdf",        source = "filePdf")
    @Mapping(target = "fileTxt",        source = "fileTxt")
    @Mapping(target = "fileTif",        source = "fileTif")
    @Mapping(target = "annoEdizione",   source = "annoEdizione")
    @Mapping(target = "stato",          source = "stato")
    @Mapping(target = "oblio",          source = "oblio")
    @Mapping(target = "dataOblio",      source = "dataOblio")
    @Mapping(target = "notaOblio",      source = "notaOblio")
    PaginaMetadata toPaginaMetadata(GdpPagina entity);

    List<PaginaMetadata> toPaginaMetadataList(List<GdpPagina> pagine);

    // -------------------------------------------------------------------------
    // EdizioneXml (oggetto radice)
    // -------------------------------------------------------------------------

    default EdizioneXml toEdizioneXml(GdpTestata testata, GdpEdizione edizione, List<GdpPagina> pagine) {
        EdizioneXml xml = new EdizioneXml();
        xml.setTestata(toTestataMetadata(testata));
        xml.setEdizione(toEdizioneMetadata(edizione));
        xml.setPagine(toPaginaMetadataList(pagine));
        return xml;
    }

    // -------------------------------------------------------------------------
    // Conversioni di tipo: LocalDate → Date (richiesto da JAXB)
    // -------------------------------------------------------------------------

    default Date map(LocalDate value) {
        return value != null
                ? Date.from(value.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant())
                : null;
    }
}