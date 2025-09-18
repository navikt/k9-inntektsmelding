package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.FagsakInfo;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.K9SakTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.FunksjonellException;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(ArbeidsgiverinitiertDialogRest.BASE_PATH)
public class ArbeidsgiverinitiertDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverinitiertDialogRest.class);

    public static final String BASE_PATH = "/arbeidsgiverinitiert";
    private static final String HENT_ARBEIDSFORHOLD = "/arbeidsforhold";
    private static final String HENT_OPPLYSNINGER = "/opplysninger";

    private GrunnlagTjeneste grunnlagTjeneste;
    private PersonTjeneste personTjeneste;
    private K9SakTjeneste k9SakTjeneste;
    private boolean erProd = true;

    ArbeidsgiverinitiertDialogRest() {
        // CDI
    }

    @Inject
    public ArbeidsgiverinitiertDialogRest(GrunnlagTjeneste grunnlagTjeneste, PersonTjeneste personTjeneste, K9SakTjeneste k9SakTjeneste) {
        this.grunnlagTjeneste = grunnlagTjeneste;
        this.personTjeneste = personTjeneste;
        this.k9SakTjeneste = k9SakTjeneste;
        this.erProd = Environment.current().isProd();
    }

    @POST
    @Path(HENT_ARBEIDSFORHOLD)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsforhold(@Valid @NotNull HentArbeidsforholdRequest request) {
        if (erProd) {
            throw new IllegalStateException("Ugyldig kall på restpunkt som ikke er lansert");
        }
        LOG.info("Henter arbeidsforhold for søker");

        // Sjekk at person finnes
        PersonInfo personInfo = personTjeneste.hentPersonFraIdent(request.fødselsnummer());
        if (personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        // Sjekk at søker har sak i k9-sak
        List<FagsakInfo> fagsakerIK9Sak =  k9SakTjeneste.hentFagsakInfo(request.ytelseType(), request.fødselsnummer());
        var finnesSakIK9 = fagsakerIK9Sak.stream().anyMatch(fagsak -> fagsak.gyldigPeriode().inneholderDato(request.førsteFraværsdag()));
        if (!finnesSakIK9) {
            var feilmelding = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen", request.ytelseType());
            throw new FunksjonellException("INGEN_SAK_FUNNET", feilmelding, null, null);
        }

        var response = grunnlagTjeneste.finnArbeidsforholdForFnr(request.fødselsnummer(), request.ytelseType(), request.førsteFraværsdag());
        return response.map(d ->Response.ok(d).build()).orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path(HENT_OPPLYSNINGER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysninger(@Valid @NotNull OpplysningerRequestDto request) {
        if (erProd) {
            throw new IllegalStateException("Ugyldig kall på restpunkt som ikke er lansert");
        }
        LOG.info("Henter opplysninger for søker");
        var hentOpplysningerResponse = grunnlagTjeneste.hentOpplysningerForNyansatt(request.fødselsnummer(), request.ytelseType(), request.førsteFraværsdag(), request.organisasjonsnummer());
        return Response.ok(hentOpplysningerResponse).build();
    }
}
