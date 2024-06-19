package no.nav.familie.inntektsmelding.forespørsel.rest;

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
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
@Transactional
@Path(SakRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class SakRest {
    public static final String BASE_PATH = "/sak";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    public SakRest() {
    }

    @Inject
    public SakRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("/ferdigstill")
    @Operation(description = "Ferdigstiller en sak på min side arbeidsgiver", tags = "sak")
    public Response ferdigstillSak(SakRequest request) {
        forespørselBehandlingTjeneste.ferdigstillSak(new AktørIdEntitet(request.aktørId().id()),
            new OrganisasjonsnummerDto(request.orgnummer().orgnr()), KodeverkMapper.mapYtelsetype(request.ytelsetype()), request.saksnummer());
        return Response.ok().build();
    }

}

