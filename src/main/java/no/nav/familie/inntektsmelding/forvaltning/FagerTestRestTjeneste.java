package no.nav.familie.inntektsmelding.forvaltning;

import java.net.URI;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import no.nav.familie.inntektsmelding.forepørsel.rest.OpprettForespørselRequest;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjon;
import no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.Merkelapp;
import no.nav.familie.inntektsmelding.typer.YtelseTypeDto;
import no.nav.foreldrepenger.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@ApplicationScoped
@Transactional
@Path(FagerTestRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class FagerTestRestTjeneste {
    static final String BASE_PATH = "/test/fager";

    private ArbeidsgiverNotifikasjon notifikasjon;
    private URI skjemaLenke;

    public FagerTestRestTjeneste() {
    }

    @Inject
    public FagerTestRestTjeneste(ArbeidsgiverNotifikasjon notifikasjon, @KonfigVerdi(value = "inntektsmelding.skjema.lenke") URI skjemaLenke) {
        this.notifikasjon = notifikasjon;
        this.skjemaLenke = UriBuilder.fromUri(skjemaLenke).path("ny").path("8068c43c-5ed7-4d0b-91c7-b8fa8c306bb3").build()
;    }


    @POST
    @UtenAutentisering
    @Path("/sak/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en forespørsel om inntektsmelding", tags = "forespørsel")
    public Response opprettForespørsel(OpprettForespørselRequest request) {
        var sakId = notifikasjon.opprettSak(request.saksnummer().getSaksnr(),
            finnMerkelapp(request.ytelsetype()),
            request.orgnummer().orgnr(),
            "Inntektsmelding for TEST TESTERSEN: f." + request.aktørId().id(),
            this.skjemaLenke);

        return Response.ok(sakId).build();
    }

    @GET
    @UtenAutentisering
    @Path("/sak/hent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en forespørsel om inntektsmelding", tags = "forespørsel")
    public Response opprettForespørsel(@QueryParam("grupperingsid") String grupperingsid, @QueryParam("merkelapp") Merkelapp merkelapp) {
        var sak = notifikasjon.hentSak(grupperingsid, merkelapp);
        return Response.ok(sak).build();
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

}

