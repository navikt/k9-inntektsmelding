package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
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
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTjeneste;
import no.nav.familie.inntektsmelding.typer.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.YtelseTypeMapper;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;

@ApplicationScoped
@Transactional
@Path(ForespørselRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
public class ForespørselRest {
    public static final String BASE_PATH = "/foresporsel";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private ForespørselTjeneste forespørselTjeneste;

    public ForespørselRest() {
    }

    @Inject
    public ForespørselRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste, ForespørselTjeneste forespørselTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.forespørselTjeneste = forespørselTjeneste;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter en forespørsel om inntektsmelding", tags = "forespørsel")
    public Response createForespørsel(OpprettForespørselRequest request) {
        forespørselBehandlingTjeneste.håndterInnkommendeForespørsel(request.skjæringstidspunkt(), YtelseTypeMapper.map(request.ytelsetype()),
            request.aktørId(), request.orgnummer(), request.saksnummer());
        return Response.ok().build();
    }

    @GET
    @Path("/{uuid}")
    @Operation(description = "Henter en forespørsel for gitt UUID", tags = "forespørsel")
    public Response readForespørsel(@PathParam("uuid") UUID forespørselUUID) {
        return Response.ok(forespørselTjeneste.finnForespørsel(forespørselUUID).map(ForespørselRest::mapTilDto).orElseThrow()).build();
    }

    record ForespørselDto(UUID uuid, OrganisasjonsnummerDto organisasjonsnummer, LocalDate skjæringstidspunkt, AktørIdDto brukerAktørId, YtelseTypeDto ytelseType) {}

    static ForespørselDto mapTilDto(ForespørselEntitet entitet) {
        return new ForespørselDto(
            entitet.getUuid(),
            new OrganisasjonsnummerDto(entitet.getOrganisasjonsnummer()),
            entitet.getSkjæringstidspunkt(),
            new AktørIdDto(entitet.getBrukerAktørId()),
            YtelseTypeMapper.map(entitet.getYtelseType()));
    }
}

