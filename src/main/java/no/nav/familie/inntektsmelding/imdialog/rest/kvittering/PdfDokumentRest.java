package no.nav.familie.inntektsmelding.imdialog.rest.kvittering;

import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.K9DokgenTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;


/**
 * Resttjeneste som serverer pdf til bruk i skjermbilder, som bekreftelse på innsending av inntektsmelding i dialogporten og arbeidsgiverportalen
 */
@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(PdfDokumentRest.BASE_PATH)
public class PdfDokumentRest {
    public static final String BASE_PATH = "/pdf";
    public static final String INNTEKTSMELDING_PATH = "/inntektsmelding";
    public static final String INNTEKTSMELDING_FULL_PATH = BASE_PATH + INNTEKTSMELDING_PATH;
    private static final Logger LOG = LoggerFactory.getLogger(PdfDokumentRest.class);
    private static final String APPLICATION_PDF = "application/pdf";
    private static final String CONTENT_DISPOSITION = "Content-Disposition";

    private Tilgang tilgang;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private K9DokgenTjeneste k9DokgenTjeneste;

    PdfDokumentRest() {
        // CDI
    }

    @Inject
    public PdfDokumentRest(Tilgang tilgang,
                           InntektsmeldingTjeneste inntektsmeldingTjeneste,
                           K9DokgenTjeneste k9DokgenTjeneste) {
        this.tilgang = tilgang;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.k9DokgenTjeneste = k9DokgenTjeneste;
    }

    @GET
    @Path(INNTEKTSMELDING_PATH + "/{uuid}")
    @Produces(APPLICATION_PDF)
    @Tilgangskontrollert
    public Response hentInnsendtInntektsmeldingPdf(@NotNull @Valid @PathParam("uuid") UUID inntektsmeldingUuid) {
        tilgang.sjekkAtArbeidsgiverHarTilgangTilInntektsmelding(inntektsmeldingUuid);
        Optional<InntektsmeldingEntitet> im = inntektsmeldingTjeneste.hentInntektsmelding(inntektsmeldingUuid);

        if (im.isEmpty()) {
            LOG.info("Finner ikke inntektsmelding med uuid {}", inntektsmeldingUuid);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        byte[] pdf = k9DokgenTjeneste.mapDataOgGenererPdf(im.get());
        Ytelsetype ytelsetekst = im.get().getYtelsetype();
        String siste12TegnFraUuid = inntektsmeldingUuid.toString().substring(inntektsmeldingUuid.toString().length() - 12);

        Response.ResponseBuilder responseBuilder = Response.ok(pdf);
        responseBuilder.type(APPLICATION_PDF);
        responseBuilder.header(CONTENT_DISPOSITION, String.format("attachment; filename=inntektsmelding-%s-%s.pdf", ytelsetekst, siste12TegnFraUuid));
        LOG.info("Returnerer pdf for inntektsmelding med id {}", inntektsmeldingUuid);

        return responseBuilder.build();
    }
}
