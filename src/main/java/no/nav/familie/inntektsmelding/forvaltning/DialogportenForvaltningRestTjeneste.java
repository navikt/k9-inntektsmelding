package no.nav.familie.inntektsmelding.forvaltning;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.DialogportenKlient;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten.DialogportenTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.util.InputValideringRegex;

@ApplicationScoped
@Path(DialogportenForvaltningRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@AutentisertMedAzure
public class DialogportenForvaltningRestTjeneste {
    public static final String BASE_PATH = "/dialogporten";
    private static final Logger LOG = LoggerFactory.getLogger(DialogportenForvaltningRestTjeneste.class);
    private static final boolean IS_PROD = Environment.current().isProd();
    private Tilgang tilgang;
    private DialogportenKlient dialogportenKlient;
    private DialogportenTjeneste dialogportenTjeneste;
    private ForespørselRepository forespørselRepository;

    DialogportenForvaltningRestTjeneste() {
        // REST CDI
    }

    @Inject
    public DialogportenForvaltningRestTjeneste(Tilgang tilgang, DialogportenKlient dialogportenKlient, DialogportenTjeneste dialogportenTjeneste,
                                               ForespørselRepository forespørselRepository) {
        this.tilgang = tilgang;
        this.dialogportenKlient = dialogportenKlient;
        this.dialogportenTjeneste = dialogportenTjeneste;
        this.forespørselRepository = forespørselRepository;
    }

    @POST
    @Path("/opprettDialog")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Ny Dialog i Dialogporten med en Inntektsmelding transmission", tags = "dialogporten")
    @Tilgangskontrollert
    public Response opprettDialog(@NotNull @Valid OpprettNyDialogDto opprettNyDialogDto) {
        if (IS_PROD) {
            throw new IllegalStateException("Kan ikke opprette dialog i produksjon. Bruk testmiljø for dette.");
        }
        sjekkAtKallerHarRollenDrift();
        ForespørselEntitet forespørsel = forespørselRepository.hentForespørsel(opprettNyDialogDto.forespørselUuid())
            .orElseThrow(() -> new IllegalStateException("Finner ikke forespørsel med uuid " + opprettNyDialogDto.forespørselUuid()));

        LOG.info("Oppretter en dialog for forespørselUuid {} og organisasjonsnummer {}",
            opprettNyDialogDto.forespørselUuid(),
            forespørsel.getOrganisasjonsnummer());
        dialogportenTjeneste.opprettForespørselDialogporten(
            opprettNyDialogDto.forespørselUuid(),
            new ArbeidsgiverDto(forespørsel.getOrganisasjonsnummer()),
            forespørsel.getAktørId(),
            forespørsel.getYtelseType(),
            forespørsel.getSkjæringstidspunkt());
        return Response.accepted().build();
    }

    @POST
    @Path("/ferdigstillerDialog")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdaterer dialogen i dialogporten til mottatt ", tags = "dialogporten")
    @Tilgangskontrollert
    public Response ferdigstillerDialog(@NotNull @Pattern(regexp = InputValideringRegex.FRITEKST) @Valid String dialogUuid) {
        if (IS_PROD) {
            throw new IllegalStateException("Kan ikke ferdigstille dialog i produksjon. Bruk testmiljø for dette.");
        }
        sjekkAtKallerHarRollenDrift();
        LOG.info("Oppdatere en dialog med dialogUuid {}", dialogUuid);

        final UUID parsedDialogUuid;
        try {
            parsedDialogUuid = UUID.fromString(dialogUuid);
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity("Ugyldig dialogUuid")
                .build();
        }

        dialogportenKlient.ferdigstillDialog(parsedDialogUuid,
            null,
            "Sakstittel",
            Ytelsetype.PLEIEPENGER_SYKT_BARN,
            LocalDate.now(),
            Optional.empty(),
            null);
        return Response.ok().build();
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }

    public record OpprettNyDialogDto(@NotNull @Valid UUID forespørselUuid) {
    }
}
