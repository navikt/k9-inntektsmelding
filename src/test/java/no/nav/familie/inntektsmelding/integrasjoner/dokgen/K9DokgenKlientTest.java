package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class K9DokgenKlientTest {

    RestClient restClient = mock(RestClient.class);

    @Test
    void skal_generere_pdf() throws URISyntaxException {
        K9DokgenKlient k9DokgenKlient = new K9DokgenKlient(restClient);
        when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
        var bytes = k9DokgenKlient.genererPdf(new InntektsmeldingPdfData());
        assertThat(bytes).isNotEmpty();
    }
}
