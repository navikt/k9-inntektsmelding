package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MerkelappTest {

    @Test
    void getBeskrivelse() {
        assertThat(Merkelapp.INNTEKTSMELDING_OMP.getBeskrivelse()).isEqualTo("nntektsmelding omsorgspenger");
        assertThat(Merkelapp.INNTEKTSMELDING_OPP.getBeskrivelse()).isEqualTo("Inntektsmelding opplæringspenger");
        assertThat(Merkelapp.INNTEKTSMELDING_PILS.getBeskrivelse()).isEqualTo("Inntektsmelding pleiepenger i livets sluttfase");
        assertThat(Merkelapp.INNTEKTSMELDING_PSB.getBeskrivelse()).isEqualTo("Inntektsmelding pleiepenger sykt barn");
    }

    @Test
    void values() {
        assertThat(Merkelapp.values()).hasSize(4);
    }

    @Test
    void valueOf() {
        assertThat(Merkelapp.valueOf("INNTEKTSMELDING_PSB")).isInstanceOf(Merkelapp.class).isEqualTo(Merkelapp.INNTEKTSMELDING_PSB);
    }

    @Test
    void valueOf_exception() {
        assertThrows(IllegalArgumentException.class, () -> Merkelapp.valueOf("test"));
    }
}
