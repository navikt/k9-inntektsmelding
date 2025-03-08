package no.nav.familie.inntektsmelding.forespørsel.rest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(ForespørselRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ForespørselRest {
    private static final Logger LOG = LoggerFactory.getLogger(ForespørselRest.class);
    public static final String BASE_PATH = "/foresporsel";

    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private Tilgang tilgang;

    ForespørselRest() {
        // Kun for CDI-proxy
    }

    @Inject
    public ForespørselRest(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste, Tilgang tilgang) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.tilgang = tilgang;
    }

    @POST
    @Path("/oppdater")
    @Tilgangskontrollert
    public Response oppdaterForespørsler(@Valid @NotNull OppdaterForespørslerRequest request) {
        LOG.info("Mottok forespørsel om oppdatering av inntektsmeldingoppgaver på saksnummer {}", request.fagsakSaksnummer());
        sjekkErSystemkall();

        boolean validertOk = validerOppdaterForespørslerRequest(request);
        if (!validertOk) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        forespørselBehandlingTjeneste.oppdaterForespørsler(
            KodeverkMapper.mapYtelsetype(request.ytelsetype()),
            new AktørIdEntitet(request.aktørId().id()),
            request.forespørsler(),
            request.fagsakSaksnummer()
        );

        return Response.ok().build();
    }

    private static boolean validerOppdaterForespørslerRequest(OppdaterForespørslerRequest request) {
        var unikeForespørsler = new ArrayList<>();
        var dupliserteForespørsler = new ArrayList<>();

        request.forespørsler().forEach(forespørsel -> {
            var forespørselPair = Pair.of(forespørsel.skjæringstidspunkt(), forespørsel.orgnr());
            if (!unikeForespørsler.contains(forespørselPair)) {
                unikeForespørsler.add(forespørselPair);
            } else {
                dupliserteForespørsler.add(forespørselPair);
            }
        });

        if (!dupliserteForespørsler.isEmpty()) {
            LOG.warn("Kan ikke oppdatere med duplikate forespørsler: {}", dupliserteForespørsler);
            return false;
        }

        return true;
    }

    @POST
    @Path("/lukk")
    @Tilgangskontrollert
    public Response lukkForespørsel(@Valid @NotNull LukkForespørselRequest request) {
        LOG.info("Lukk forespørsel for saksnummer {} med orgnummer {} og skjæringstidspunkt {}",
            request.fagsakSaksnummer(),
            request.orgnummer(),
            request.skjæringstidspunkt());

        sjekkErSystemkall();

        forespørselBehandlingTjeneste.lukkForespørsel(request.fagsakSaksnummer(), request.orgnummer(), request.skjæringstidspunkt());
        return Response.ok().build();
    }

    @POST
    @Path("/sett-til-utgatt")
    @Tilgangskontrollert
    public Response settForespørselTilUtgått(@Valid @NotNull LukkForespørselRequest request) {
        LOG.info("Setter forespørsel for saksnummer {} til utgått", request.fagsakSaksnummer());

        sjekkErSystemkall();

        forespørselBehandlingTjeneste.settForespørselTilUtgått(request.fagsakSaksnummer(), request.orgnummer(), request.skjæringstidspunkt());
        return Response.ok().build();
    }

    @GET
    @Path("/sak")
    @Tilgangskontrollert
    public Response hentForespørslerForSak(@Valid @NotNull @Pattern(regexp = SaksnummerDto.REGEXP) @Size(max = 19) @QueryParam("saksnummer") String saksnummer) {
        LOG.info("Henter forespørsler for saksnummer {}", saksnummer);

        sjekkErSystemkall();

        var forespørsler = forespørselBehandlingTjeneste.hentForespørslerForFagsak(new SaksnummerDto(saksnummer), null, null);
        var forespørselDtos = forespørsler.stream().map(ForespørselRest::mapTilDto).toList();

        return Response.ok(forespørselDtos).build();
    }

    record ForespørselDto(UUID uuid, OrganisasjonsnummerDto organisasjonsnummer, LocalDate skjæringstidspunkt, AktørIdDto brukerAktørId,
                          YtelseTypeDto ytelseType, ForespørselStatus status) {
    }

    static ForespørselDto mapTilDto(ForespørselEntitet entitet) {
        return new ForespørselDto(entitet.getUuid(), new OrganisasjonsnummerDto(entitet.getOrganisasjonsnummer()), entitet.getSkjæringstidspunkt(),
            new AktørIdDto(entitet.getAktørId().getAktørId()), KodeverkMapper.mapYtelsetype(entitet.getYtelseType()), entitet.getStatus());
    }

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }
}

