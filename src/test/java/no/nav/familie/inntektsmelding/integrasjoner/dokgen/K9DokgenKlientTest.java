package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;

class K9DokgenKlientTest {

    RestClient restClient = mock(RestClient.class);

    @Test
    void skal_generere_pdf() throws URISyntaxException {
        K9DokgenKlient k9DokgenKlient = new K9DokgenKlient(restClient);
        when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
        var bytes = k9DokgenKlient.genererPdfInntektsmelding(lagTestInntektsmeldingPdfRequest());
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void skal_generere_pdf_omsorgspenger_refusjon() throws URISyntaxException {
        K9DokgenKlient k9DokgenKlient = new K9DokgenKlient(restClient);
        when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
        var bytes = k9DokgenKlient.genererPdfOmsorgspengerRefusjon(lagTestOmsorgspengerPdfRequest("true"));
        assertThat(bytes).isNotEmpty();
    }

    @Test
    void skal_generere_pdf_omsorgspenger_inntektsmelding() throws URISyntaxException {
        K9DokgenKlient k9DokgenKlient = new K9DokgenKlient(restClient);
        when(restClient.sendReturnByteArray(any())).thenReturn("pdf".getBytes());
        var bytes = k9DokgenKlient.genererPdfOmsorgspengerInntektsmelding(lagTestOmsorgspengerPdfRequest("false"));
        assertThat(bytes).isNotEmpty();
    }

    private OmsorgspengerPdfRequest lagTestOmsorgspengerPdfRequest(String harUtbetaltLønn) {
        return new OmsorgspengerPdfRequest(
            "K9-INNTEKTSMELDING",
            "Test Testesen",
            "11111111111",
            "123456789",
            "Minimal AS",
            new Kontaktperson("Test Kontakt", "87654321"),
            new BigDecimal("35000"),
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            List.of(),
            List.of(),
            harUtbetaltLønn
        );
    }

    private InntektsmeldingPdfRequest lagTestInntektsmeldingPdfRequest() {
        return new InntektsmeldingPdfRequest(
            "K9-INNTEKTSMELDING",
            "Test Testesen",
            "11111111111",
            Ytelsetype.OMSORGSPENGER,
            "123456789",
            "Minimal AS",
            new Kontaktperson("Test Kontakt", "87654321"),
            "2024-01-01",
            new BigDecimal("35000"),
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            List.of(),
            List.of(),
            true,
            true,
            List.of(),
            0
        );
    }
}
