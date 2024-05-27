package no.nav.familie.inntektsmelding.database.modell;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;

@Converter(autoApply = true)
public class YtelsetypeKodeverdiConverter implements AttributeConverter<Ytelsetype, String> {
    @Override
    public String convertToDatabaseColumn(Ytelsetype attribute) {
        return attribute == null ? null : attribute.getKode();
    }

    @Override
    public Ytelsetype convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Ytelsetype.fraKode(dbData);
    }
}
