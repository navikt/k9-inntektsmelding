package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.konfig.KonfigVerdi;
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
    private final String templatePath;
    private final String templateType;

    @Inject
    public K9DokgenKlient(
        @KonfigVerdi(value = "pdf.template.path", defaultVerdi = "/template/inntektsmelding/PDFINNTEKTSMELDING") String tempatePath,
        @KonfigVerdi(value = "pdf.template.type", defaultVerdi = "/create-pdf-format-variation") String templateType
    ) {
        this(RestClient.client(), tempatePath, templateType);
    }

    public K9DokgenKlient(RestClient restClient,
                          String templatePath,
                          String templateType) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(K9DokgenKlient.class);
        this.templatePath = templatePath;
        this.templateType = templateType;
    }

    public byte[] genererPdf(InntektsmeldingPdfData dokumentdata) throws URISyntaxException {
        var endpoint = new URI(restConfig.endpoint() + templatePath + templateType);
        var request = RestRequest.newPOSTJson(dokumentdata, endpoint, restConfig);
        var pdf = restClient.sendReturnByteArray(request);

        if (pdf == null || pdf.length == 0) {
            throw new TekniskException("FPIM", "Fikk tomt svar ved kall til dokgen for generering av pdf for inntektsmelding");
        }
        return pdf;
    }
}
