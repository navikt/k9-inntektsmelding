package no.nav.familie.inntektsmelding.imdialog.rest;

import java.time.LocalDate;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagTjeneste;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingMottakTjeneste;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

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
    private static final String HENT_INNTEKTSMELDINGER_FOR_ÅR_V2 = "/inntektsmeldinger-for-aar/v2";
    private static final String SEND_INNTEKTSMELDING = "/send-inntektsmelding";
    private static final String SEND_INNTEKTSMELDING_OMS_REFUSJON = "/send-inntektsmelding/omsorgspenger-refusjon";
    private static final String SEND_INNTEKTSMELDING_OMS_REFUSJON_V2 = "/send-inntektsmelding/omsorgspenger-refusjon/v2";
    private static final String SEND_INNTEKTSMELDING_ARBEIDSGIVERINITIERT_NYANSATT = "/send-inntektsmelding/arbeidsgiverinitiert-nyansatt";
    private static final String SEND_INNTEKTSMELDING_ARBEIDSGIVERINITIERT_UREGISTRERT = "/send-inntektsmelding/arbeidsgiverinitiert-uregistrert";
    private static final String LAST_NED_PDF = "/last-ned-pdf";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private InntektsmeldingMottakTjeneste inntektsmeldingMottakTjeneste;
    private GrunnlagTjeneste grunnlagTjeneste;
    private Tilgang tilgang;
    private PersonTjeneste personTjeneste;

    InntektsmeldingDialogRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingDialogRest(InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                     InntektsmeldingMottakTjeneste inntektsmeldingMottakTjeneste,
                                     GrunnlagTjeneste grunnlagTjeneste,
                                     Tilgang tilgang,
                                     PersonTjeneste personTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.inntektsmeldingMottakTjeneste = inntektsmeldingMottakTjeneste;
        this.grunnlagTjeneste = grunnlagTjeneste;
        this.tilgang = tilgang;
        this.personTjeneste = personTjeneste;
    }

    @GET
    @Path(HENT_OPPLYSNINGER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysninger(@Valid @NotNull @QueryParam("foresporselUuid") UUID forespørselUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(forespørselUuid);

        LOG.info("Henter forespørsel med uuid {}", forespørselUuid);
        var dto = grunnlagTjeneste.hentOpplysninger(forespørselUuid);
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

    /**
     * @deprecated Bruk {@link #hentInntektsmeldingerForÅrV2(HentInntektsmeldingerForÅrV2Request)} i stedet.
     */
    @Deprecated(forRemoval = true)
    @POST
    @Path(HENT_INNTEKTSMELDINGER_FOR_ÅR)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentInntektsmeldingerForÅrPost(@NotNull @Valid HentInntektsmeldingerForÅrRequest request) {
        if (erUgyldigÅr(request.år())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Fremtidige år er ikke tillatt").build();
        }

        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(request.arbeidsgiverIdent().ident()));

        LOG.info("Henter inntektsmeldinger for år for organisasjon {}", request.arbeidsgiverIdent());

        // denne metoden vil kun bli brukt for omsorgspenger da de har fagsak som strekker seg over ett år
        var dto = inntektsmeldingTjeneste.hentInntektsmeldingerForÅr(new AktørIdEntitet(request.aktørId().id()),
            request.arbeidsgiverIdent().ident(),
            request.år(),
            Ytelsetype.OMSORGSPENGER);
        return Response.ok(dto).build();
    }

    @POST
    @Path(HENT_INNTEKTSMELDINGER_FOR_ÅR_V2)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentInntektsmeldingerForÅrV2(@NotNull @Valid HentInntektsmeldingerForÅrV2Request request) {
        if (erUgyldigÅr(request.år())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Fremtidige år er ikke tillatt").build();
        }

        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(request.arbeidsgiverIdent().ident()));

        LOG.info("Henter inntektsmeldinger for år for organisasjon {}", request.arbeidsgiverIdent());

        var aktørId = personTjeneste.finnAktørIdForPersonIdent(request.fødselsnummer().getIdent());
        if (aktørId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Fant ikke person for oppgitt fødselsnummer").build();
        }

        // denne metoden vil kun bli brukt for omsorgspenger da de har fagsak som strekker seg over ett år
        var dto = inntektsmeldingTjeneste.hentInntektsmeldingerForÅr(aktørId.get(),
            request.arbeidsgiverIdent().ident(),
            request.år(),
            Ytelsetype.OMSORGSPENGER);
        return Response.ok(dto).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmelding(@NotNull @Valid SendInntektsmeldingRequest sendInntektsmeldingRequest) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(sendInntektsmeldingRequest.foresporselUuid());
        LOG.info("Mottok inntektsmelding for forespørsel {}", sendInntektsmeldingRequest.foresporselUuid());
        return Response.ok(inntektsmeldingMottakTjeneste.mottaInntektsmelding(sendInntektsmeldingRequest)).build();
    }

    /**
     * @deprecated Bruk {@link #sendInntektsmeldingForOmsorgspengerRefusjonV2(SendInntektsmeldingOmsorgspengerRefusjonV2Request)} i stedet.
     */
    @Deprecated(forRemoval = true)
    @POST
    @Path(SEND_INNTEKTSMELDING_OMS_REFUSJON)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmeldingForOmsorgspengerRefusjon(@NotNull @Valid SendInntektsmeldingRequest sendInntektsmeldingRequest) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(sendInntektsmeldingRequest.arbeidsgiverIdent().ident()));
        LOG.info("Mottok inntektsmelding for omsorgspenger refusjon for aktørId {}", sendInntektsmeldingRequest.aktorId());
        return Response.ok(inntektsmeldingMottakTjeneste.mottaInntektsmeldingForOmsorgspengerRefusjon(sendInntektsmeldingRequest)).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING_OMS_REFUSJON_V2)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmeldingForOmsorgspengerRefusjonV2(@NotNull @Valid SendInntektsmeldingOmsorgspengerRefusjonV2Request request) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(request.arbeidsgiverIdent().ident()));

        var aktørId = personTjeneste.finnAktørIdForPersonIdent(request.fødselsnummer().getIdent());
        if (aktørId.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Fant ikke person for oppgitt fødselsnummer").build();
        }

        LOG.info("Mottok inntektsmelding for omsorgspenger refusjon for aktørId {}", aktørId.get());
        var sendInntektsmeldingRequest = request.tilSendInntektsmeldingRequest(new AktørIdDto(aktørId.get().getAktørId()));
        return Response.ok(inntektsmeldingMottakTjeneste.mottaInntektsmeldingForOmsorgspengerRefusjon(sendInntektsmeldingRequest)).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING_ARBEIDSGIVERINITIERT_NYANSATT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmeldingForArbeidsgiverinitiertNyansatt(@NotNull @Valid SendInntektsmeldingArbeidsgiverinitiertNyansattRequest request) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(request.arbeidsgiverIdent().ident()));
        LOG.info("Mottok arbeidsgiverinitiert nyansatt inntektsmelding for aktørId {}", request.aktorId());
        InntektsmeldingResponseDto response = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertNyansattInntektsmelding(request);
        return Response.ok(response).build();
    }

    @POST
    @Path(SEND_INNTEKTSMELDING_ARBEIDSGIVERINITIERT_UREGISTRERT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response sendInntektsmeldingForArbeidsgiverinitiertUregistrert(@NotNull @Valid SendInntektsmeldingRequest request) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilBedrift(new OrganisasjonsnummerDto(request.arbeidsgiverIdent().ident()));
        LOG.info("Mottok arbeidsgiverinitiert uregistrert inntektsmelding for aktørId {}", request.aktorId());
        InntektsmeldingResponseDto response = inntektsmeldingMottakTjeneste.mottaArbeidsgiverinitiertUregistrertInntektsmelding(request);
        return Response.ok(response).build();
    }


    /**
     * @deprecated Bruk {@link no.nav.familie.inntektsmelding.imdialog.rest.kvittering.PdfDokumentRest#hentInnsendtInntektsmeldingPdf(UUID)} i stedet.
     */
    @Deprecated(forRemoval = true)
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

    private static boolean erUgyldigÅr(int år) {
        return år > LocalDate.now().getYear() || år < 2020;
    }
}
