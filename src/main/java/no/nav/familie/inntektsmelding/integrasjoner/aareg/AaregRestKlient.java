package no.nav.familie.inntektsmelding.integrasjoner.aareg;

import java.net.URI;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriBuilderException;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.dto.ArbeidsforholdDto;
import no.nav.vedtak.felles.integrasjon.rest.NavHeaders;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

/*
 * Dokumentasjon https://confluence.adeo.no/display/FEL/AAREG+-+Tjeneste+REST+aareg.api
 * Swagger https://aareg-services-q2.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v1#/arbeidstaker/finnArbeidsforholdPrArbeidstaker
 * Swagger V2 https://aareg-services-q2.dev.intern.nav.no/swagger-ui/index.html?urls.primaryName=aareg.api.v2#/arbeidstaker/finnArbeidsforholdPrArbeidstaker
 */

@Dependent
@RestClientConfig(tokenConfig = TokenFlow.ADAPTIVE, endpointProperty = "aareg.rs.url",
    endpointDefault = "https://aareg-services.dev-fss-pub.nais.io",
    scopesProperty = "aareg.scopes", scopesDefault = "api://dev-fss.arbeidsforhold.aareg-services-nais/.default")
public class AaregRestKlient {

    private final RestClient restClient; // Setter på consumer-token fra STS
    private final RestConfig restConfig;

    public AaregRestKlient() {
        this(RestClient.client());
    }

    public AaregRestKlient(RestClient restClient) {
        this.restClient = restClient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<ArbeidsforholdDto> finnNåværendeArbeidsforholdForArbeidstaker(String personIdent) {
        try {
            var nå = LocalDate.now();
            var uri = lagUriForForFinnArbeidsforholdForArbeidstaker(nå, nå);
            var request = RestRequest.newGET(uri, restConfig).header(NavHeaders.HEADER_NAV_PERSONIDENT, personIdent);
            var response = restClient.send(request, ArbeidsforholdDto[].class);
            // TODO: filtrer på ansettelseperiode
            return Arrays.asList(response);
        } catch (UriBuilderException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Utviklerfeil syntax-exception for finnArbeidsforholdForArbeidstaker");
        }
    }

    /** Kun eksponert for å kunne teste URI-bygging – skal ikke brukes ellers */
    URI lagUriForForFinnArbeidsforholdForArbeidstaker(LocalDate qfom, LocalDate qtom) {
        return UriBuilder.fromUri(restConfig.endpoint())
            .path("arbeidstaker/arbeidsforhold")
            .queryParam("rapporteringsordning", "A_ORDNINGEN")
            .queryParam("historikk", "true")
            .queryParam("sporingsinformasjon", "false")
            .build();
    }

}
