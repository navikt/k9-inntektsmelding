package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import no.nav.familie.inntektsmelding.koder.NaturalytelseType;

/**
 * Leser både ELEKTRISK_KOMMUNIKASJON og ELEKTRONISK_KOMMUNIKASJON, men skriver fortsatt det gamle navnet
 * slik at pods på eldre versjon kan lese raden under utrulling. Byttes til nytt navn i steg 2 sammen med Flyway-migrering.
 */
@Converter
public class NaturalytelseTypeKonverterer implements AttributeConverter<NaturalytelseType, String> {

    private static final String GAMMELT_NAVN = "ELEKTRISK_KOMMUNIKASJON";

    @Override
    public String convertToDatabaseColumn(NaturalytelseType type) {
        if (type == null) {
            return null;
        }
        return type == NaturalytelseType.ELEKTRONISK_KOMMUNIKASJON ? GAMMELT_NAVN : type.name();
    }

    @Override
    public NaturalytelseType convertToEntityAttribute(String verdi) {
        if (verdi == null) {
            return null;
        }
        return GAMMELT_NAVN.equals(verdi) ? NaturalytelseType.ELEKTRONISK_KOMMUNIKASJON : NaturalytelseType.valueOf(verdi);
    }
}
