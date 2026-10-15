package no.nav.familie.inntektsmelding.imdialog.modell;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.koder.NaturalytelseType;

class NaturalytelseTypeKonvertererTest {

    private final NaturalytelseTypeKonverterer konverterer = new NaturalytelseTypeKonverterer();

    @Test
    void leser_både_gammelt_og_nytt_navn() {
        assertThat(konverterer.convertToEntityAttribute("ELEKTRISK_KOMMUNIKASJON")).isEqualTo(NaturalytelseType.ELEKTRONISK_KOMMUNIKASJON);
        assertThat(konverterer.convertToEntityAttribute("ELEKTRONISK_KOMMUNIKASJON")).isEqualTo(NaturalytelseType.ELEKTRONISK_KOMMUNIKASJON);
    }

    @Test
    void skriver_gammelt_navn_for_elektronisk_kommunikasjon() {
        assertThat(konverterer.convertToDatabaseColumn(NaturalytelseType.ELEKTRONISK_KOMMUNIKASJON)).isEqualTo("ELEKTRISK_KOMMUNIKASJON");
    }

    @Test
    void øvrige_typer_uendret() {
        assertThat(konverterer.convertToDatabaseColumn(NaturalytelseType.BIL)).isEqualTo("BIL");
        assertThat(konverterer.convertToEntityAttribute("BIL")).isEqualTo(NaturalytelseType.BIL);
        assertThat(konverterer.convertToDatabaseColumn(null)).isNull();
    }
}
