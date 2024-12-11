package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FpDokgenKlientTest {

    RestClient restClient = mock(RestClient.class);

    @Test
    public void skal_generere_pdf() throws URISyntaxException {
        FpDokgenKlient fpDokgenKlient = new FpDokgenKlient(restClient, "/path", "/path");
        when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
        fpDokgenKlient.genererPdf(new InntektsmeldingPdfData());
    }
}
