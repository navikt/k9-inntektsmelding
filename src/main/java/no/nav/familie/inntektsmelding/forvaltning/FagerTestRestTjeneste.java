package no.nav.familie.inntektsmelding.forvaltning;

import java.net.URI;
import java.time.OffsetDateTime;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.familie.inntektsmelding.forespørsel.rest.OpprettForespørselRequest;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.SaksStatus;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@Transactional
@Path(FagerTestRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class FagerTestRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FagerTestRestTjeneste.class);

    static final String BASE_PATH = "/test/fager";

    private ArbeidsgiverNotifikasjon notifikasjon;
    private URI skjemaLenke;

    public FagerTestRestTjeneste() {
    }

    @Inject
    public FagerTestRestTjeneste(ArbeidsgiverNotifikasjon notifikasjon, @KonfigVerdi(value = "inntektsmelding.skjema.lenke") URI skjemaLenke) {
        this.notifikasjon = notifikasjon;
        this.skjemaLenke = UriBuilder.fromUri(skjemaLenke).path("ny").path("8068c43c-5ed7-4d0b-91c7-b8fa8c306bb3").build();
    }

    @POST
    @UtenAutentisering
    @Path("/sak/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en ny sak i fager", tags = "test")
    public Response opprettForespørsel(@Valid @NotNull OpprettForespørselRequest request) {
        var sakId = notifikasjon.opprettSak(request.saksnummer().saksnr(),
            finnMerkelapp(request.ytelsetype()),
            request.orgnummer().orgnr(),
            "Inntektsmelding for TEST TESTERSEN: f." + request.aktørId().id(),
            this.skjemaLenke);

        return Response.ok(sakId).build();
    }

    @GET
    @UtenAutentisering
    @Path("/sak/hentMedGrupperingsid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter sak fra fager med Grupperingsid og Merkelapp", tags = "test")
    public Response hentSakMedGrupperingsid(@QueryParam("grupperingsid") @NotNull String grupperingsid, @QueryParam("merkelapp") @NotNull Merkelapp merkelapp) {
        var sak = notifikasjon.hentSakMedGrupperingsid(grupperingsid, merkelapp);
        return Response.ok(sak).build();
    }

    @GET
    @UtenAutentisering
    @Path("/sak/hent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter sak fra fager med ID", tags = "test")
    public Response hentSakMedId(@QueryParam("sakId") @NotNull String sakId) {
        var sak = notifikasjon.hentSak(sakId);
        return Response.ok(sak).build();
    }

    @POST
    @UtenAutentisering
    @Path("/sak/status/oppdater")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer sak status i fager med ID", tags = "test")
    public Response oppdaterSakStatusMedId(@Valid @NotNull OppdaterStatusSakRequest request) {
        var statusId = notifikasjon.oppdaterSakStatus(request.sakId(), request.status(), request.overstyrtStatusTekst());
        return Response.ok(statusId).build();
    }

    public record OppdaterStatusSakRequest(String sakId, SaksStatus status, String overstyrtStatusTekst) {}

    @POST
    @UtenAutentisering
    @Path("/sak/status/oppdaterMedGrupperingsid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer sak status i fager med Grupperingsid og Merkelapp", tags = "test")
    public Response oppdaterSakStatusMedGrupperingsid(@Valid @NotNull OppdaterStatusSakMedGrupperingsidRequest request) {
        var statusId = notifikasjon.oppdaterSakStatusMedGrupperingsId(request.grupperingsid(), request.merkelapp(), request.status(), request.overstyrtStatusTekst());
        return Response.ok(statusId).build();
    }

    public record OppdaterStatusSakMedGrupperingsidRequest(String grupperingsid, Merkelapp merkelapp, SaksStatus status, String overstyrtStatusTekst) {}

    @POST
    @UtenAutentisering
    @Path("/oppgave/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en ny oppgave i fager", tags = "test")
    public Response opprettOppgave(@Valid @NotNull OpprettForespørselRequest request) {
        var eksternId = String.join("-", request.saksnummer().saksnr(), request.orgnummer().orgnr()); // mulig man trenger arbforholdId også.
        LOG.info("FAGER: eksternId={}", eksternId);
        var oppgaveId = notifikasjon.opprettOppgave(
            request.saksnummer().saksnr(),
            finnMerkelapp(request.ytelsetype()),
            eksternId,
            request.orgnummer().orgnr(),
            "NAV trenger inntektsmelding for å kunne behandle saken til din ansatt",
            this.skjemaLenke);

        return Response.ok(oppgaveId).build();
    }

    @POST
    @UtenAutentisering
    @Path("/oppgave/utfoer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Utfør en oppgave i fager med Id", tags = "test")
    public Response opprettUtfoerById(@Valid @NotNull LukkOppgaveRequest request) {
        var oppgaveId = notifikasjon.lukkOppgave(
            request.oppgaveId(),
            OffsetDateTime.now());

        return Response.ok(oppgaveId).build();
    }

    public record LukkOppgaveRequest(String oppgaveId) {}

    @POST
    @UtenAutentisering
    @Path("/oppgave/utfoerMedGrupperingsid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Utfør en oppgave i fager med EksternId og merkelapp", tags = "test")
    public Response opprettUtfoerMedEksternId(@Valid @NotNull LukkOppgaveMedEksternIdRequest request) {
        var oppgaveId = notifikasjon.lukkOppgaveByEksternId(
            request.eksternId(),
            request.merkelapp(),
            OffsetDateTime.now());

        return Response.ok(oppgaveId).build();
    }

    public record LukkOppgaveMedEksternIdRequest(String eksternId, Merkelapp merkelapp) {}

    private Merkelapp finnMerkelapp(YtelseTypeDto ytelsetype) {
        return switch (ytelsetype) {
            case FORELDREPENGER -> Merkelapp.INNTEKTSMELDING_FP;
            case PLEIEPENGER_SYKT_BARN -> Merkelapp.INNTEKTSMELDING_PSB;
            case OMSORGSPENGER -> Merkelapp.INNTEKTSMELDING_OMP;
            case SVANGERSKAPSPENGER -> Merkelapp.INNTEKTSMELDING_SVP;
            case PLEIEPENGER_NÆRSTÅENDE -> Merkelapp.INNTEKTSMELDING_PILS;
            case OPPLÆRINGSPENGER -> Merkelapp.INNTEKTSMELDING_OPP;
        };
    }

}

