package no.nav.familie.inntektsmelding.integrasjoner.fpsak;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;

import no.nav.vedtak.exception.IntegrasjonException;
import no.nav.vedtak.felles.integrasjon.rest.FpApplication;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@ApplicationScoped
@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, application = FpApplication.FPSAK)
public class FpsakKlient {
    private static final String FPSAK_API = "/api";

    private final RestClient restClient;
    private final RestConfig restConfig;

    FpsakKlient() {
        this.restClient = RestClient.client();
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public List<SøkersFraværsperiode> hentSøkersFravær(String saksnummer) {
        var uri = uri("/fpinntektsmelding/sokersFravaer", saksnummer);
        var restRequest = RestRequest.newGET(uri, restConfig);
        try {
            var respons = restClient.sendReturnOptional(restRequest, SøkersFraværDto.class);
            return respons.map(SøkersFraværDto::perioder).orElse(List.of()).stream().map(p -> new SøkersFraværsperiode(p.fom, p.tom)).toList();
        } catch (Exception e) {
            throw new IntegrasjonException("FP-81473",
                "Feil ved kall til fpsak, kunne ikke hente søkers fraværsperioder", e);
        }
    }

    private URI uri(String path, String saksnummer) {
        return UriBuilder.fromUri(restConfig.endpoint()).path(FPSAK_API).path(path).queryParam("saksnummer", saksnummer).build();
    }

    protected record SøkersFraværDto(List<FraværsperiodeDto> perioder) {
        public record FraværsperiodeDto(LocalDate fom, LocalDate tom){};
    }
}
