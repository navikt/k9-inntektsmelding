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
import no.nav.familie.inntektsmelding.database.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.typer.AktørId;
import no.nav.familie.inntektsmelding.typer.Organisasjonsnummer;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@ApplicationScoped
@Transactional
@Path(ForespørselRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ForespørselRestTjeneste {

    private ForespørselTjeneste forespørselTjeneste;

    public ForespørselRestTjeneste() {
    }

    @Inject
    public ForespørselRestTjeneste(ForespørselTjeneste forespørselTjeneste) {
        this.forespørselTjeneste = forespørselTjeneste;
    }

    public static final String BASE_PATH = "/foresporsel";

    @POST
    @UtenAutentisering
    @Path("/opprett")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en forespørsel om inntektsmelding", tags = "forespørsel")
    public Response opprettForespørsel(OpprettForespørselRequest request) {
        forespørselTjeneste.opprettForespørsel(request.skjæringstidspunkt(), request.ytelsetype(), new AktørId(request.aktørId().getId()),
            new Organisasjonsnummer(request.orgnummer().orgnr()), request.saksnummer());

        return Response.ok().build();
    }


}

