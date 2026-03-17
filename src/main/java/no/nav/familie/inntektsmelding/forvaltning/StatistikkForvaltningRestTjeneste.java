package no.nav.familie.inntektsmelding.forvaltning;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
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

    StatistikkForvaltningRestTjeneste() {
        // REST CDI
    }

    @Inject
    public StatistikkForvaltningRestTjeneste(Tilgang tilgang, InntektsmeldingRepository inntektsmeldingRepository) {
        this.tilgang = tilgang;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
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

    protected record AntallInntektsmeldingerResponse(long antall) {
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
