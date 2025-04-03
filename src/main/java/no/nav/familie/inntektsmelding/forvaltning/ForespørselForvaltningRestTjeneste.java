package no.nav.familie.inntektsmelding.forvaltning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedAzure
@OpenAPIDefinition(tags = @Tag(name = "Inntektsmelding", description = "Undersøk om alle inntektsmeldinger er knyttet til en forespørsel",))
@RequestScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
@Path("/inntektsmelding")
public class ForespørselForvaltningRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForespørselForvaltningRestTjeneste.class);

    private Tilgang tilgang;
    private InntektsmeldingRepository inntektsmeldingRepository;

    ForespørselForvaltningRestTjeneste() {
        // REST CDI
    }

    @Inject
    public ForespørselForvaltningRestTjeneste(Tilgang tilgang, InntektsmeldingRepository inntektsmeldingRepository) {
        this.tilgang = tilgang;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    @POST
    @Path("/im-uten-forespørsel")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(
        description = "Finn antall inntektsmeldinger som ikker har en forespørsel",
        summary = "Finn antall inntektsmeldinger som ikker har en forespørsel",
        tags = "oppgaver",
        responses = {
            @ApiResponse(responseCode = "200", description = "Gir antall inntektsmeldinger som ikke har en forespørsel",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = Long.class))),
            @ApiResponse(responseCode = "500", description = "Feilet pga ukjent feil eller tekniske/funksjonelle feil")
        })
    @Tilgangskontrollert
    public Response erAlleInntektsmeldingKnyttetTilEnForespørsel() {
        sjekkAtKallerHarRollenDrift();

        var resultat = inntektsmeldingRepository.antallInntektsmeldingerUtenForespørsel();

        return Response.ok(resultat).build();
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
