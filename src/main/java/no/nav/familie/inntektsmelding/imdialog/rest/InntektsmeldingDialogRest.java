package no.nav.familie.inntektsmelding.imdialog.rest;

import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;

import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(InntektsmeldingDialogRest.BASE_PATH)
public class InntektsmeldingDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingDialogRest.class);

    public static final String BASE_PATH = "/imdialog";
    private static final String HENT_OPPLYSNINGER = "/opplysninger";
    private static final String HENT_INNTEKTSMELDINGER_FOR_OPPGAVE = "/inntektsmeldinger";
    private static final String HENT_INNTEKTSMELDINGER_FOR_ÅR = "/inntektsmeldinger-for-aar";
    private static final String SEND_INNTEKTSMELDING = "/send-inntektsmelding";
    private static final String SEND_INNTEKTSMELDING_OMS_REFUSJON = "/send-inntektsmelding/omsorgspenger-refusjon";
    private static final String LAST_NED_PDF = "/last-ned-pdf";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private Tilgang tilgang;

    InntektsmeldingDialogRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingDialogRest(InntektsmeldingTjeneste inntektsmeldingTjeneste, Tilgang tilgang) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.tilgang = tilgang;
    }

    @GET
    @Path(HENT_OPPLYSNINGER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysninger(@Valid @NotNull @QueryParam("foresporselUuid") UUID forespørselUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);

        LOG.info("Henter forespørsel med uuid {}", forespørselUuid);
        var dto = inntektsmeldingTjeneste.lagDialogDto(forespørselUuid);
        return Response.ok(dto).build();

    }

    @GET
    @Path(HENT_INNTEKTSMELDINGER_FOR_OPPGAVE)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentInntektsmeldingerForOppgave(@NotNull @Valid @QueryParam("foresporselUuid") UUID forespørselUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);

        LOG.info("Henter inntektsmeldinger for forespørsel {}", forespørselUuid);
        var dto = inntektsmeldingTjeneste.hentInntektsmeldinger(forespørselUuid);
        return Response.ok(dto).build();
    }

    @GET
    @Path(HENT_INNTEKTSMELDINGER_FOR_ÅR)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentInntektsmeldingerForÅr(@NotNull @Valid @QueryParam("aktørId") AktørIdDto aktorId,
                                               @NotNull @Valid @QueryParam("arbeidsgiverIdent") ArbeidsgiverDto arbeidsgiverIdent,
                                               @NotNull @Valid @QueryParam("år") int år) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(arbeidsgiverIdent.ident()));

        LOG.info("Henter inntektsmeldinger for år for organisasjon {}", arbeidsgiverIdent.ident());

        // denne metoden vil kun bli brukt for omsorgspenger da de har fagsak som strekker seg over ett år
        var dto = inntektsmeldingTjeneste.hentInntektsmeldingerForÅr(new AktørIdEntitet(aktorId.id()),
            arbeidsgiverIdent.ident(),
            år,
            Ytelsetype.OMSORGSPENGER);
        return Response.ok(dto).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmelding(@NotNull @Valid SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(sendInntektsmeldingRequestDto.foresporselUuid());
        LOG.info("Mottok inntektsmelding for forespørsel {}", sendInntektsmeldingRequestDto.foresporselUuid());
        return Response.ok(inntektsmeldingTjeneste.mottaInntektsmelding(sendInntektsmeldingRequestDto)).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING_OMS_REFUSJON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmeldingForOmsorgspengerRefusjon(@NotNull @Valid SendInntektsmeldingRequestDto sendInntektsmeldingRequestDto) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(sendInntektsmeldingRequestDto.arbeidsgiverIdent().ident()));
        LOG.info("Mottok inntektsmelding for omsorgspenger refusjon for aktørId {}", sendInntektsmeldingRequestDto.aktorId());
        return Response.ok(inntektsmeldingTjeneste.mottaInntektsmeldingForOmsorgspengerRefusjon(sendInntektsmeldingRequestDto)).build();

    }

    @GET
    @Path(LAST_NED_PDF)
    @Produces("application/pdf")
    @Tilgangskontrollert
    public Response lastNedPDF(@NotNull @Valid @QueryParam("id") long inntektsmeldingId) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(inntektsmeldingId);

        LOG.info("Henter inntektsmelding for id {}", inntektsmeldingId);
        var pdf = inntektsmeldingTjeneste.hentPDF(inntektsmeldingId);

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=inntektsmelding.pdf");
        return responseBuilder.build();
    }
}
