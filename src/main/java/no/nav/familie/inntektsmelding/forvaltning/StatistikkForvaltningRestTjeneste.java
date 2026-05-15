package no.nav.familie.inntektsmelding.forvaltning;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.koder.ForespørselStatus;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedAzure
@OpenAPIDefinition(tags = @Tag(name = "statistikk", description = "Statistikk over inntektsmeldinger"))
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/forvaltning-statistikk")
public class StatistikkForvaltningRestTjeneste {

    private Tilgang tilgang;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private ForespørselRepository forespørselRepository;

    StatistikkForvaltningRestTjeneste() {
        // REST CDI
    }

    @Inject
    public StatistikkForvaltningRestTjeneste(Tilgang tilgang,
                                             InntektsmeldingRepository inntektsmeldingRepository,
                                             ForespørselRepository forespørselRepository) {
        this.tilgang = tilgang;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.forespørselRepository = forespørselRepository;
    }

    @GET
    @Path("/antall-inntektsmeldinger")
    @Operation(description = "Henter antall rader i InntektsmeldingEntitet tabellen", summary = "Henter antall inntektsmeldinger.", tags = "statistikk", responses = {
        @ApiResponse(responseCode = "200", description = "Antall inntektsmeldinger hentet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentAntallInntektsmeldinger() {
        sjekkAtKallerHarRollenDrift();
        long antall = inntektsmeldingRepository.tellAntallInntektsmeldinger();
        return Response.ok(new AntallInntektsmeldingerResponse(antall)).build();
    }

    @GET
    @Path("/antall-omsorgspenger-refusjon-inntektsmeldinger")
    @Operation(description = "Teller antall inntektsmeldinger med InntektsmeldingType = OMSORGSPENGER_REFUSJON", summary = "Teller antall omsorgspenger refusjon inntektsmeldinger.", tags = "statistikk", responses = {
        @ApiResponse(responseCode = "200", description = "Antall inntektsmeldinger hentet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentAntallOmsorgspengerRefusjonInntektsmeldinger() {
        sjekkAtKallerHarRollenDrift();
        long antall = inntektsmeldingRepository.tellAntallOmsorgspengerRefusjonInntektsmeldinger();
        return Response.ok(new AntallInntektsmeldingerResponse(antall)).build();
    }

    @GET
    @Path("/antall-utledet-omsorgspenger-refusjon-inntektsmeldinger")
    @Operation(description = "Teller antall inntektsmeldinger med ytelse_type = OMSORGSPENGER og maaned_refusjon IS NOT NULL", summary = "Teller antall utledet omsorgspenger refusjon inntektsmeldinger.", tags = "statistikk", responses = {
        @ApiResponse(responseCode = "200", description = "Antall inntektsmeldinger hentet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentAntallUtledetOmsorgspengerRefusjonInntektsmeldinger() {
        sjekkAtKallerHarRollenDrift();
        long antall = inntektsmeldingRepository.tellAntallUtledetOmsorgspengerRefusjonInntektsmeldinger();
        return Response.ok(new AntallInntektsmeldingerResponse(antall)).build();
    }

    @GET
    @Path("/antall-utledet-omsorgspenger-refusjon-inntektsmeldinger-fra-foresporsel")
    @Operation(description = "Teller antall inntektsmeldinger der tilknyttet forespørsel har ForespørselType = OMSORGSPENGER_REFUSJON", summary = "Teller antall utledet omsorgspenger refusjon inntektsmeldinger fra forespørsel.", tags = "statistikk", responses = {
        @ApiResponse(responseCode = "200", description = "Antall inntektsmeldinger hentet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentAntallUtledetOmsorgspengerRefusjonInntektsmeldingerFraForespørsel() {
        sjekkAtKallerHarRollenDrift();
        long antall = inntektsmeldingRepository.tellAntallUtledetOmsorgspengerRefusjonInntektsmeldingerFraForespørsel();
        return Response.ok(new AntallInntektsmeldingerResponse(antall)).build();
    }

    @GET
    @Path("/antall-ferdige-forespoersler")
    @Operation(description = "Teller antall forespørsler med status FERDIG i angitt periode", summary = "Teller antall ferdige forespørsler.", tags = "statistikk", responses = {
        @ApiResponse(responseCode = "200", description = "Antall ferdige forespørsler hentet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentAntallFerdigeForespørsler(@QueryParam("fraDato") LocalDate fraDato, @QueryParam("tilDato") LocalDate tilDato) {
        sjekkAtKallerHarRollenDrift();
        validerDatoParametere(fraDato, tilDato);
        long antall = forespørselRepository.tellForespørslerMedStatus(fraDato, tilDato, ForespørselStatus.FERDIG);
        return Response.ok(new AntallForespørslerResponse(antall)).build();
    }

    @GET
    @Path("/antall-under-behandling-forespoersler")
    @Operation(description = "Teller antall forespørsler med status UNDER_BEHANDLING i angitt periode", summary = "Teller antall forespørsler under behandling.", tags = "statistikk", responses = {
        @ApiResponse(responseCode = "200", description = "Antall forespørsler under behandling hentet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentAntallUnderBehandlingForespørsler(@QueryParam("fraDato") LocalDate fraDato, @QueryParam("tilDato") LocalDate tilDato) {
        sjekkAtKallerHarRollenDrift();
        validerDatoParametere(fraDato, tilDato);
        long antall = forespørselRepository.tellForespørslerMedStatus(fraDato, tilDato, ForespørselStatus.UNDER_BEHANDLING);
        return Response.ok(new AntallForespørslerResponse(antall)).build();
    }

    @GET
    @Path("/dager-til-lukking")
    @Operation(description = "Henter fordeling av antall dager fra opprettet til lukket (endret_tid - opprettet_tid) for ferdige forespørsler i angitt periode", summary = "Henter dager til lukking fordeling.", tags = "statistikk", responses = {
        @ApiResponse(responseCode = "200", description = "Fordeling hentet", content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
    })
    @Tilgangskontrollert
    public Response hentDagerTilLukking(@QueryParam("fraDato") LocalDate fraDato, @QueryParam("tilDato") LocalDate tilDato) {
        sjekkAtKallerHarRollenDrift();
        validerDatoParametere(fraDato, tilDato);
        var rader = forespørselRepository.hentDagerTilLukkingFordeling(fraDato, tilDato).stream()
            .map(row -> new DagerTilLukking(((Number) row[0]).intValue(), ((Number) row[1]).longValue()))
            .toList();
        return Response.ok(new DagerTilLukkingResponse(rader)).build();
    }

    protected record AntallInntektsmeldingerResponse(long antall) {
    }

    protected record AntallForespørslerResponse(long antall) {
    }

    protected record DagerTilLukking(int antallDager, long antallForespørsler) {
    }

    protected record DagerTilLukkingResponse(List<DagerTilLukking> dagerTilLukking) {
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }

    private void validerDatoParametere(LocalDate fraDato, LocalDate tilDato) {
        if (fraDato == null || tilDato == null) {
            throw new IllegalArgumentException("fraDato og tilDato må være satt");
        }
        if (fraDato.isAfter(tilDato)) {
            throw new IllegalArgumentException("fraDato kan ikke være etter tilDato");
        }
    }
}
