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
    endpointProperty = "k9pdfgen.url",
    endpointDefault = "http://k9-pdfgen-psb",
    application = FpApplication.NONFP)
public class K9PdfgenKlient implements DokgenKlient {
    private final RestClient restClient;
    private final RestConfig restConfig;

    private final String INNTEKTSMELDING_PATH = "/api/v1/genpdf/inntektsmelding/inntektsmelding";
    private final String REFUSJONSKRAV_NYANSATT = "/api/v1/genpdf/inntektsmelding/inntektsmelding-refusjonskrav";
    private final String OMSORGSPENGER_REFUSJON_PATH = "/api/v1/genpdf/inntektsmelding/omsorgspenger_refusjon";
    private final String OMSORGSPENGER_INNTEKTSMELDING_PATH = "/api/v1/genpdf/inntektsmelding/omsorgspenger_inntektsmelding";

    @Inject
    public K9PdfgenKlient(
    ) {
        this(RestClient.client());
    }

    public K9PdfgenKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(K9PdfgenKlient.class);
    }

    public byte[] genererPdfInntektsmelding(InntektsmeldingPdfRequest pdfRequest) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + INNTEKTSMELDING_PATH);
        var request = RestRequest.newPOSTJson(pdfRequest, endpoint, restConfig);
        return genererPdf(request);
    }

    public byte[] genererPdfRefusjonskravNyansatt(RefusjonskravNyansattData pdfRequest) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + REFUSJONSKRAV_NYANSATT);
        var request = RestRequest.newPOSTJson(pdfRequest, endpoint, restConfig);
        return genererPdf(request);
    }

    public byte[] genererPdfOmsorgspengerRefusjon(OmsorgspengerRefusjonPdfRequest pdfRequest) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + OMSORGSPENGER_REFUSJON_PATH);
        var request = RestRequest.newPOSTJson(pdfRequest, endpoint, restConfig);
        return genererPdf(request);
    }

    public byte[] genererPdfOmsorgspengerInntektsmelding(OmsorgspengerInntektsmeldingPdfRequest pdfRequest) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + OMSORGSPENGER_INNTEKTSMELDING_PATH);
        var request = RestRequest.newPOSTJson(pdfRequest, endpoint, restConfig);
        return genererPdf(request);
    }

    private byte[] genererPdf(RestRequest request) {
        var pdf = restClient.sendReturnByteArray(request);

        if (pdf == null || pdf.length == 0) {
            throw new TekniskException("K9IM", "Fikk tomt svar ved kall til pdfgen for generering av pdf");
        }
        return pdf;
    }
}
