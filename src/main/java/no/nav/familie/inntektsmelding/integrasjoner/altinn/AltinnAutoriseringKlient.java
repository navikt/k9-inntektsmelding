package no.nav.familie.inntektsmelding.integrasjoner.altinn;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "altinn.url", scopesProperty = "altinn.scopes")
public class AltinnAutoriseringKlient {
    private static final String SERVICE_CODE = "4936";
    private static final String SERVICE_EDITION = "1";
    private static final String FILTER_AKTIVE_BEDRIFTER = "Type ne 'Person' and Status eq 'Active'";
    /** Altinn takler ikke høyere limit */
    static final int ALTINN_SIZE_LIMIT = 500;
    /** Antar at ingen har tilganger til flere enn dette, for å unngå uendelig antall kall ved feil */
    private static final int ALTINN_TOTAL_SIZE_LIMIT = 100_000;

    private final RestClient restClient;
    private final RestConfig restConfig;

    public AltinnAutoriseringKlient() {
        this(RestClient.client());
    }

    public AltinnAutoriseringKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public void sjekkTilgang(String orgnr) {
        List<AltinnReportee> altinnReportees = gjørKallMedPagineringOgRetry();

        if (altinnReportees.stream().noneMatch(reportee -> orgnr.equals(reportee.organizationNumber()))) {
            throw new RuntimeException("Innlogget bruker har ikke tilgang til organisasjon");
        }
    }

    private List<AltinnReportee> gjørKallMedPagineringOgRetry() {
        List<AltinnReportee> altinnReportees = new ArrayList<>();

        int skip = 0;
        while (skip < ALTINN_TOTAL_SIZE_LIMIT) {
            List<AltinnReportee> respons = gjørKallMedRetry(skip);
            altinnReportees.addAll(respons);

            if (respons.size() < ALTINN_SIZE_LIMIT) {
                break;
            } else {
                skip += ALTINN_SIZE_LIMIT;
            }
        }

        return altinnReportees;
    }

    private List<AltinnReportee> gjørKallMedRetry(int skip) {
        List<AltinnReportee> reportees;
        try {
            reportees = gjørKall(skip);
        } catch (IntegrasjonException e) {
            // Gjør 1 retry
            reportees = gjørKall(skip);
        }
        return reportees;
    }

    private List<AltinnReportee> gjørKall(int skip) {
        URI uri = UriBuilder.fromUri(restConfig.endpoint())
            .queryParam("serviceCode", SERVICE_CODE)
            .queryParam("serviceEdition", SERVICE_EDITION)
            .queryParam("$filter", FILTER_AKTIVE_BEDRIFTER)
            .queryParam("$top", ALTINN_SIZE_LIMIT)
            .queryParam("$skip", skip)
            .queryParam("X-Consumer-ID", KontekstHolder.getKontekst().getKonsumentId())
            .build();
        RestRequest request = RestRequest.newGET(uri, restConfig);
        try {
            return restClient.sendReturnList(request, AltinnReportee.class);
        } catch (RuntimeException e) {
            throw new IntegrasjonException("FP-TBD",
                "Feil ved kall til altinn-rettigheter-proxy. Meld til #team_fager hvis dette skjer over lengre tidsperiode.", e);
        }
    }
}
