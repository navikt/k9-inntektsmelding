package no.nav.familie.inntektsmelding.forvaltning;

import java.net.URI;
import java.time.OffsetDateTime;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.familie.inntektsmelding.forespørsel.rest.OpprettForespørselRequest;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.SaksStatus;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.exception.ManglerTilgangException;

/**
 * @deprecated Disse endepunktene brukes til å utforske muligheter i arbeidsgiver portalen og vil feile i produksjon.
 * Fjernes til slutt fra applikasjonen.
 */
@AutentisertMedAzure
@Deprecated(forRemoval = true)
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path(FagerTestRestTjeneste.BASE_PATH)
public class FagerTestRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(FagerTestRestTjeneste.class);
    private static final boolean IS_PROD = Environment.current().isProd();

    static final String BASE_PATH = "/test/fager";

    private ArbeidsgiverNotifikasjon notifikasjon;
    private Tilgang tilgangsstyring;

    private URI skjemaLenke;

    public FagerTestRestTjeneste() {
        // CDI proxy
    }

    @Inject
    public FagerTestRestTjeneste(ArbeidsgiverNotifikasjon notifikasjon,
                                 Tilgang tilgangsstyring,
                                 @KonfigVerdi(value = "inntektsmelding.skjema.lenke") URI skjemaLenke) {
        this.notifikasjon = notifikasjon;
        this.tilgangsstyring = tilgangsstyring;
        this.skjemaLenke = UriBuilder.fromUri(skjemaLenke).path("ny").path("8068c43c-5ed7-4d0b-91c7-b8fa8c306bb3").build();
    }

    @POST
    @Path("/sak/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en ny sak i fager", tags = "test")
    public Response opprettForespørsel(@Valid @NotNull OpprettForespørselRequest request) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtSaksbehandlerHarRollenDrift();

        var sakId = notifikasjon.opprettSak(request.fagsakSaksnummer().saksnr(), finnMerkelapp(request.ytelsetype()), request.orgnummer().orgnr(),
            "Inntektsmelding for TEST TESTERSEN: f." + request.aktørId().id(), this.skjemaLenke);

        return Response.ok(sakId).build();
    }

    @GET
    @Path("/sak/hentMedGrupperingsid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter sak fra fager med Grupperingsid og Merkelapp", tags = "test")
    public Response hentSakMedGrupperingsid(@QueryParam("grupperingsid") @NotNull String grupperingsid,
                                            @QueryParam("merkelapp") @NotNull Merkelapp merkelapp) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtSaksbehandlerHarRollenDrift();

        var sak = notifikasjon.hentSakMedGrupperingsid(grupperingsid, merkelapp);
        return Response.ok(sak).build();
    }

    @GET
    @Path("/sak/hent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter sak fra fager med ID", tags = "test")
    public Response hentSakMedId(@QueryParam("sakId") @NotNull String sakId) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtSaksbehandlerHarRollenDrift();

        var sak = notifikasjon.hentSak(sakId);
        return Response.ok(sak).build();
    }

    @POST
    @Path("/sak/status/oppdater")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer sak status i fager med ID", tags = "test")
    public Response oppdaterSakStatusMedId(@Valid @NotNull OppdaterStatusSakRequest request) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtSaksbehandlerHarRollenDrift();

        var statusId = notifikasjon.oppdaterSakStatus(request.sakId(), request.status(), request.overstyrtStatusTekst());
        return Response.ok(statusId).build();
    }

    public record OppdaterStatusSakRequest(String sakId, SaksStatus status, String overstyrtStatusTekst) {
    }

    @POST
    @Path("/sak/status/oppdaterMedGrupperingsid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer sak status i fager med Grupperingsid og Merkelapp", tags = "test")
    public Response oppdaterSakStatusMedGrupperingsid(@Valid @NotNull OppdaterStatusSakMedGrupperingsidRequest request) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtSaksbehandlerHarRollenDrift();

        var statusId = notifikasjon.oppdaterSakStatusMedGrupperingsId(request.grupperingsid(), request.merkelapp(), request.status(),
            request.overstyrtStatusTekst());
        return Response.ok(statusId).build();
    }

    public record OppdaterStatusSakMedGrupperingsidRequest(String grupperingsid, Merkelapp merkelapp, SaksStatus status,
                                                           String overstyrtStatusTekst) {
    }

    @POST
    @Path("/oppgave/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en ny oppgave i fager", tags = "test")
    public Response opprettOppgave(@Valid @NotNull OpprettForespørselRequest request) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtSaksbehandlerHarRollenDrift();

        var eksternId = String.join("-", request.fagsakSaksnummer().saksnr(), request.orgnummer().orgnr()); // mulig man trenger arbforholdId også.
        LOG.info("FAGER: eksternId={}", eksternId);
        var oppgaveId = notifikasjon.opprettOppgave(request.fagsakSaksnummer().saksnr(), finnMerkelapp(request.ytelsetype()), eksternId,
            request.orgnummer().orgnr(), "NAV trenger inntektsmelding for å kunne behandle saken til din ansatt", this.skjemaLenke);

        return Response.ok(oppgaveId).build();
    }

    @POST
    @Path("/oppgave/utfoer")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Utfør en oppgave i fager med Id", tags = "test")
    public Response opprettUtfoerById(@Valid @NotNull LukkOppgaveRequest request) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtSaksbehandlerHarRollenDrift();

        var oppgaveId = notifikasjon.oppgaveUtfoert(request.oppgaveId(), OffsetDateTime.now());

        return Response.ok(oppgaveId).build();
    }

    public record LukkOppgaveRequest(String oppgaveId) {
    }

    @POST
    @Path("/oppgave/utfoerMedGrupperingsid")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Utfør en oppgave i fager med EksternId og merkelapp", tags = "test")
    public Response opprettUtfoerMedEksternId(@Valid @NotNull LukkOppgaveMedEksternIdRequest request) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtSaksbehandlerHarRollenDrift();

        var oppgaveId = notifikasjon.oppgaveUtfoertByEksternId(request.eksternId(), request.merkelapp(), OffsetDateTime.now());

        return Response.ok(oppgaveId).build();
    }

    public record LukkOppgaveMedEksternIdRequest(String eksternId, Merkelapp merkelapp) {
    }

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

    private void sjekkAtSaksbehandlerHarRollenDrift() {
        tilgangsstyring.sjekkAtSaksbehandlerHarRollenDrift();
    }

}

