package no.nav.familie.inntektsmelding.forepørsel.rest;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.familie.inntektsmelding.database.tjenester.InnkommendeForespørselTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.YtelseTypeDto;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@ApplicationScoped
@Transactional
@Path(ForespørselRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ForespørselRestTjeneste {

    private InnkommendeForespørselTjeneste innkommendeForespørselTjeneste;

    public ForespørselRestTjeneste() {
    }

    @Inject
    public ForespørselRestTjeneste(InnkommendeForespørselTjeneste innkommendeForespørselTjeneste) {
        this.innkommendeForespørselTjeneste = innkommendeForespørselTjeneste;
    }

    public static final String BASE_PATH = "/foresporsel";

    @POST
    @UtenAutentisering
    @Path("/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en forespørsel om inntektsmelding", tags = "forespørsel")
    public Response opprettForespørsel(OpprettForespørselRequest request) {
        innkommendeForespørselTjeneste.håndterInnkommendeForespørsel(request.skjæringstidspunkt(), map(request.ytelsetype()), new AktørIdDto(request.aktørId().id()),
            new OrganisasjonsnummerDto(request.orgnummer().orgnr()), request.saksnummer());
        return Response.ok().build();
    }

    private static Ytelsetype map(YtelseTypeDto ytelseTypeDto) {
        return switch (ytelseTypeDto) {
            case FORELDREPENGER -> Ytelsetype.FORELDREPENGER;
            case SVANGERSKAPSPENGER -> Ytelsetype.SVANGERSKAPSPENGER;
            case OMSORGSPENGER -> Ytelsetype.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> Ytelsetype.OPPLÆRINGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
        };
    }

}

