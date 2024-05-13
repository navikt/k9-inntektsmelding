package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonErrorHandler.handleError;
import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonErrorHandler.handleValidationError;

import java.net.http.HttpRequest;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLRequest;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResult;

import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.Error;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NyOppgaveMutationRequest;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NyOppgaveMutationResponse;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NyOppgaveResultatResponseProjection;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NyOppgaveVellykket;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NySakMutationRequest;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NySakMutationResponse;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NySakResultatResponseProjection;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NySakVellykket;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.OppgaveUtfoertMutationRequest;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.OppgaveUtfoertMutationResponse;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.OppgaveUtfoertResultatResponseProjection;
import no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.OppgaveUtfoertVellykket;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestClientConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestConfig;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;
import no.nav.vedtak.felles.integrasjon.rest.TokenFlow;

@RestClientConfig(tokenConfig = TokenFlow.AZUREAD_CC, endpointProperty = "arbeidsgiver.notifikasjon.url", endpointDefault = "https://ag-notifikasjon-produsent-api.intern.nav.no", scopesProperty = "arbeidsgiver.notifikasjon.scopes", scopesDefault = "api://prod-gcp.fager.notifikasjon-produsent-api/.default")
class ArbeidsgiverNotifikasjonKlient {

    private static final String ERROR_RESPONSE = "F-102030";

    private final RestClient restKlient;
    private final RestConfig restConfig;

    public ArbeidsgiverNotifikasjonKlient(RestClient restKlient) {
        this.restKlient = restKlient;
        this.restConfig = RestConfig.forClient(this.getClass());
    }

    public String opprettSak(NySakMutationRequest request, NySakResultatResponseProjection projection) {
        var resultat = query(new GraphQLRequest(request, projection), NySakMutationResponse.class).nySak();
        if (resultat instanceof NySakVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "opprettelse av ny sak");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public String opprettOppgave(NyOppgaveMutationRequest request, NyOppgaveResultatResponseProjection projection) {
        var resultat = query(new GraphQLRequest(request, projection), NyOppgaveMutationResponse.class).nyOppgave();
        if (resultat instanceof NyOppgaveVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "opprettelse av ny oppgave");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    public String lukkOppgave(OppgaveUtfoertMutationRequest request, OppgaveUtfoertResultatResponseProjection projection) {
        var resultat = query(new GraphQLRequest(request, projection), OppgaveUtfoertMutationResponse.class).oppgaveUtfoert();
        if (resultat instanceof OppgaveUtfoertVellykket vellykket) {
            return vellykket.getId();
        } else {
            loggFeilmelding((Error) resultat, "lukking av oppgave");
        }
        throw new IllegalStateException("Utviklerfeil: Ulovlig tilstand.");
    }

    private <T extends GraphQLResult<?>> T query(GraphQLRequest req, Class<T> clazz) {
        var method = new RestRequest.Method(RestRequest.WebMethod.POST, HttpRequest.BodyPublishers.ofString(req.toHttpJsonBody()));
        var restRequest = RestRequest.newRequest(method, restConfig.endpoint(), restConfig);
        var res = restKlient.send(restRequest, clazz);
        if (res.hasErrors()) {
            return handleError(res.getErrors(), restConfig.endpoint(), ERROR_RESPONSE);
        }
        return res;
    }

    private static void loggFeilmelding(Error feil, String action) {
        handleValidationError("Funksjonellfeil", feil.getFeilmelding(), action);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [endpoint=" + restConfig.endpoint() + "]";
    }
}
