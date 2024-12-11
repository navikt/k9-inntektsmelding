package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URISyntaxException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FpDokgenKlientTest {

    @Test
    public void skal_generere_pdf() throws URISyntaxException {
        try (var mockRest = Mockito.mockStatic(RestClient.class)) {
            RestClient restClient = mock(RestClient.class);
            mockRest.when(RestClient::client).thenReturn(restClient);
            FpDokgenKlient fpDokgenKlient = new FpDokgenKlient("/path", "/path");
            when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
            fpDokgenKlient.genererPdf(new InntektsmeldingPdfData());
        }
    }
}
