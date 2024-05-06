package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class ArbeidsgiverNotifikasjonTjenesteTest {

    @Test
    @Disabled
    void testKallToFager() {
        var klient = new ArbeidsgiverNotifikasjonKlient(RestClient.client());
        var tjeneste = new ArbeidsgiverNotifikasjonTjeneste(klient);

        var resultat = tjeneste.opprettOppgave("Ble", URI.create("http://haha.com"), Merkelapp.INNTEKTSMELDING_FP, "2342342334");

        assertThat(resultat).isNotNull();
    }
}
