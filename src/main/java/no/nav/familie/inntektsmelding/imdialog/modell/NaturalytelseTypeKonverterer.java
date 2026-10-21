package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.familie.inntektsmelding.koder.NaturalytelseType;

/** Leser også gammelt navn ELEKTRISK_KOMMUNIKASJON; fjernes etter Flyway-migrering. */
@Converter
public class NaturalytelseTypeKonverterer implements AttributeConverter<NaturalytelseType, String> {

    private static final String GAMMELT_NAVN = "ELEKTRISK_KOMMUNIKASJON";

    @Override
    public String convertToDatabaseColumn(NaturalytelseType type) {
        if (type == null) {
            return null;
        }
        return type.name();
    }

    @Override
    public NaturalytelseType convertToEntityAttribute(String verdi) {
        if (verdi == null) {
            return null;
        }
        return GAMMELT_NAVN.equals(verdi) ? NaturalytelseType.ELEKTRONISK_KOMMUNIKASJON : NaturalytelseType.valueOf(verdi);
    }
}
