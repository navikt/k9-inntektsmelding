package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.NO_AUTH_NEEDED,
    endpointProperty = "k9dokgen.url",
    endpointDefault = "http://k9-dokgen",
    application = FpApplication.NONFP)
public class K9DokgenKlient {
    private final RestClient restClient;
    private final RestConfig restConfig;

    private final String INNTEKTSMELDING_PATH = "/template/inntektsmelding/PDFINNTEKTSMELDING/create-pdf-format-variation";
    private final String REFUSJONSKRAV_NYANSATT = "/template/inntektsmelding-refusjonskrav/PDFINNTEKTSMELDING/create-pdf-format-variation";
    private final String OMSORGSPENGER_REFUSJON_PATH = "/template/omsorgspenger_refusjon/PDFINNTEKTSMELDING/create-pdf-format-variation";
    private final String OMSORGSPENGER_INNTEKTSMELDING_PATH = "/template/omsorgspenger_inntektsmelding/PDFINNTEKTSMELDING/create-pdf-format-variation";

    @Inject
    public K9DokgenKlient(
    ) {
        this(RestClient.client());
    }

    public K9DokgenKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(K9DokgenKlient.class);
    }

    public byte[] genererPdfInntektsmelding(InntektsmeldingPdfData dokumentdata) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + INNTEKTSMELDING_PATH);
        var request = RestRequest.newPOSTJson(dokumentdata, endpoint, restConfig);
        return genererPdf(request);
    }

    public byte[] genererPdfRefusjonskravNyansatt(RefusjonskravNyansattData dokumentdata) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + REFUSJONSKRAV_NYANSATT);
        var request = RestRequest.newPOSTJson(dokumentdata, endpoint, restConfig);
        return genererPdf(request);
    }

    public byte[] genererPdfOmsorgspengerRefusjon(OmsorgspengerRefusjonPdfData dokumentdata) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + OMSORGSPENGER_REFUSJON_PATH);
        var request = RestRequest.newPOSTJson(dokumentdata, endpoint, restConfig);
        return genererPdf(request);
    }

    public byte[] genererPdfOmsorgspengerInntektsmelding(OmsorgspengerInntektsmeldingPdfData dokumentdata) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + OMSORGSPENGER_INNTEKTSMELDING_PATH);
        var request = RestRequest.newPOSTJson(dokumentdata, endpoint, restConfig);
        return genererPdf(request);
    }

    private byte[] genererPdf(RestRequest request) {
        var pdf = restClient.sendReturnByteArray(request);

        if (pdf == null || pdf.length == 0) {
            throw new TekniskException("K9IM", "Fikk tomt svar ved kall til dokgen for generering av pdf");
        }
        return pdf;
    }
}
