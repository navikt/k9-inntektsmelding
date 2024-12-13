package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class FpDokgenKlientTest {

    RestClient restClient = mock(RestClient.class);

    @Test
    void skal_generere_pdf() throws URISyntaxException {
        FpDokgenKlient fpDokgenKlient = new FpDokgenKlient(restClient, "/path", "/path");
        when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
        var bytes = fpDokgenKlient.genererPdf(new InntektsmeldingPdfData());
        assertThat(bytes).isNotEmpty();
    }
}
