package no.nav.familie.inntektsmelding.forvaltning;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselTekster;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.LukkeÅrsak;
import no.nav.familie.inntektsmelding.forvaltning.rest.ForvaltningForespørselDto;
import no.nav.familie.inntektsmelding.server.audit.SporingsloggTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.SaksnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt;

@AutentisertMedAzure
@OpenAPIDefinition(tags = @Tag(name = "oppgaver", description = "Hånstering av feilopprettede saker / oppgaver i arbeidsgiverportalen"))
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path(OppgaverForvaltningRestTjeneste.BASE_PATH)
public class OppgaverForvaltningRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(OppgaverForvaltningRestTjeneste.class);
    public static final String BASE_PATH = "/forvaltning-oppgaver";
    private static final String SLETT_OPPGAVE_PATH = "/slettOppgave";
    private static final String GJENOPPRETT_LUKKET_FORESPØRSEL_PATH = "/gjenopprett-lukket-foresporsel";
    private static final String HENT_FORESPØRSLER_FOR_SAK_PATH = "/foresporsler";

    private Tilgang tilgang;
    private SporingsloggTjeneste sporingsloggTjeneste;
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;

    OppgaverForvaltningRestTjeneste() {
        // REST CDI
    }

    @Inject
    public OppgaverForvaltningRestTjeneste(Tilgang tilgang,
                                           SporingsloggTjeneste sporingsloggTjeneste,
                                           ForespørselBehandlingTjeneste forespørselBehandlingTjeneste) {
        this.tilgang = tilgang;
        this.sporingsloggTjeneste = sporingsloggTjeneste;
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
    }

    @POST
    @Path(SLETT_OPPGAVE_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Sletter en oppgave i arbeidsgiverportalen", summary = "Sletter en oppgave i arbeidsgiverportalen.", tags = "oppgaver", responses = {
        @ApiResponse(responseCode = "202", description = "Oppgaven er slettet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response slettOppgave(
        @Parameter(description = "Informasjon om oppgaven") @Valid SlettOppgaveRequest inputDto) {
        sjekkAtKallerHarRollenDrift();
        LOG.info("Sletter oppgave med saksnummer {}", inputDto.saksnummer());
        forespørselBehandlingTjeneste.slettForespørsel(inputDto.saksnummer(), inputDto.orgnr(), null);
        return Response.ok().build();
    }

    protected record SlettOppgaveRequest(@Valid @NotNull SaksnummerDto saksnummer, @Valid @NotNull OrganisasjonsnummerDto orgnr) {
    }

    @POST
    @Path(GJENOPPRETT_LUKKET_FORESPØRSEL_PATH)
    @Consumes(MediaType.APPLICATION_JSON)
@Operation(description = "Gjenoppretter forespørsel som er lukket av LPS eller altinn", summary = "Gjenoppretter forespørsel som er lukket av LPS eller altinn.", tags = "oppgaver", responses = {
    @ApiResponse(responseCode = "200", description = "Gjenoppretting utført", content = @Content(mediaType = "application/json")),
    @ApiResponse(responseCode = "404", description = "Forespørsel ikke funnet"),
    @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
})
    @Tilgangskontrollert
    public Response gjenopprettLukketForesporsel(
        @Parameter(description = "Informasjon om oppgaven") @Valid @NotNull GjenopprettLukketForesporselRequest request) {
        sjekkAtKallerHarRollenDrift();
        Optional<ForespørselEntitet> forespørselEntitet = forespørselBehandlingTjeneste.hentForespørsel(request.forespørselUuid());
        if (forespørselEntitet.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String tilleggsInfo = ForespørselTekster.lagTilleggsInformasjon(LukkeÅrsak.EKSTERN_INNSENDING, forespørselEntitet.get().getSkjæringstidspunkt());
        forespørselBehandlingTjeneste.gjenåpneForespørsel(forespørselEntitet.get(), tilleggsInfo);

        LOG.info("Gjenopprettet forespørsel med uuid {}", request.forespørselUuid());
        return Response.ok().build();
    }

    protected record GjenopprettLukketForesporselRequest(@NotNull @Valid UUID forespørselUuid) {
    }

    @GET
    @Path(HENT_FORESPØRSLER_FOR_SAK_PATH)
    @Operation(description = "Henter forespørsler for et saksnummer", summary = "Henter forespørsler for et saksnummer.", tags = "oppgaver", responses = {
        @ApiResponse(responseCode = "200", description = "Forespørsler hentet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentForespørslerForSak(
        @Parameter(description = "Saksnummer det skal hentes forespørsler for") @Valid @NotNull @Pattern(regexp = SaksnummerDto.REGEXP) @Size(max = 19) @QueryParam("saksnummer") String saksnummer) {
        LOG.info("Henter forespørsler for saksnummer {}", saksnummer);

        sjekkAtKallerHarRollenDriftOgTilgangTilSak(saksnummer);
        List<ForespørselEntitet> forespørsler = forespørselBehandlingTjeneste.hentForespørslerForFagsak(new SaksnummerDto(saksnummer), null, null);

        sporingsloggTjeneste.logg(BASE_PATH + HENT_FORESPØRSLER_FOR_SAK_PATH,
            new AktørIdDto(hentAktørIdFraForespørsler(forespørsler).getAktørId()),
            new SaksnummerDto(saksnummer));

        List<ForvaltningForespørselDto> response = forespørsler.stream()
            .map(f -> new ForvaltningForespørselDto(
                f.getUuid(),
                f.getSkjæringstidspunkt(),
                f.getOrganisasjonsnummer(),
                f.getAktørId().getAktørId(),
                f.getYtelseType().toString(),
                f.getStatus(),
                f.getOpprettetTidspunkt(),
                f.getDialogportenUuid().orElse(null),
                f.getEtterspurtePerioder()))
            .toList();

        return Response.ok(response).build();
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }

    private void sjekkAtKallerHarRollenDriftOgTilgangTilSak(String saksnummer) {
        tilgang.sjekkAtAnsattHarRollenDrift();
        tilgang.sjekkAtAnsattHarTilgangTilSak(saksnummer, BeskyttetRessursActionAttributt.READ);
    }

    private AktørIdEntitet hentAktørIdFraForespørsler(List<ForespørselEntitet> forespørsler) {
        if (forespørsler.isEmpty()) {
            throw new IllegalArgumentException("Forespørsler kan ikke være tom");
        }

        AktørIdEntitet førsteAktørId = forespørsler.getFirst().getAktørId();
        boolean alleHarSammeAktørId = forespørsler.stream().allMatch(f -> f.getAktørId().equals(førsteAktørId));
        if (!alleHarSammeAktørId) {
            throw new IllegalStateException("Alle forespørsler må ha samme aktørId");
        }

        return førsteAktørId;
    }
}
