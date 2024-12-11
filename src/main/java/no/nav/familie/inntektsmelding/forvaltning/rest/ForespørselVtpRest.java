package no.nav.familie.inntektsmelding.forvaltning.rest;

import io.swagger.v3.oas.annotations.Parameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.foreldrepenger.konfig.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(ForespørselVtpRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForespørselVtpRest {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselVtpRest.class);
    public static final String BASE_PATH = "/foresporsel";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    ForespørselVtpRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public ForespørselVtpRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    // Dette endpointet brukes til verdikjedetesting, ikke i bruk i prod
    @GET
    @Path("/list/{saksnummer}")
    @Tilgangskontrollert
    public Response finnForespoerselForSaksnummer(
        @Parameter(description = "Saksnummer det skal listes ut forespørsler for") @Valid @NotNull
        @PathParam("saksnummer") SaksnummerDto saksnummer) {
        if (!(Environment.current().isLocal() || Environment.current().isVTP())) {
            throw new RuntimeException("Endepunkt for listing av forespørsler per sak skal kun brukes for verdikjedetesting, lokalt eller på github");
        }
        LOG.info("Mottok forespørsel om uuid for forespørsel for sak {}", saksnummer);
        var forespørsler = forespørselBehandlingTjeneste.finnForespørslerForFagsak(saksnummer);
        return Response.ok(new ListForespørslerResponse(forespørsler)).build();
    }
}

