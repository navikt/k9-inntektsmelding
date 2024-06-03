package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.InnkommendeForespørselTjeneste;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.YtelseTypeMapper;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@ApplicationScoped
@Transactional
@Path(ForespørselRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ForespørselRest {

    private InnkommendeForespørselTjeneste innkommendeForespørselTjeneste;
    private ForespørselTjeneste forespørselTjeneste;

    public ForespørselRest() {
    }

    @Inject
    public ForespørselRest(InnkommendeForespørselTjeneste innkommendeForespørselTjeneste, ForespørselTjeneste forespørselTjeneste) {
        this.innkommendeForespørselTjeneste = innkommendeForespørselTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    public static final String BASE_PATH = "/foresporsel";

    @POST
    @UtenAutentisering
    @Path("/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en forespørsel om inntektsmelding", tags = "forespørsel")
    public Response opprettForespørsel(OpprettForespørselRequest request) {
        innkommendeForespørselTjeneste.håndterInnkommendeForespørsel(request.skjæringstidspunkt(), YtelseTypeMapper.map(request.ytelsetype()),
            new AktørIdDto(request.aktørId().id()), new OrganisasjonsnummerDto(request.orgnummer().orgnr()), request.saksnummer());
        return Response.ok().build();
    }

    @GET
    @UtenAutentisering
    @Path("/{forespørselUUID}")
    @Operation(description = "Henter en forespørsel for gitt UUID", tags = "forespørsel")
    public Response opprettForespørsel(@PathParam("forespørselUUID") UUID forespørselUUID) {
        return Response.ok(forespørselTjeneste.finnForespørsel(forespørselUUID)).build();
    }

}

