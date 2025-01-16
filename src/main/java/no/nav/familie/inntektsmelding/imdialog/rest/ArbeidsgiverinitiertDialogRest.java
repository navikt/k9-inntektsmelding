package no.nav.familie.inntektsmelding.imdialog.rest;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(ArbeidsgiverinitiertDialogRest.BASE_PATH)
public class ArbeidsgiverinitiertDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverinitiertDialogRest.class);

    public static final String BASE_PATH = "/arbeidsgiverinitiert";
    private static final String HENT_ARBEIDSFORHOLD = "/arbeidsforhold";

    private InntektsmeldingTjeneste inntektsmeldingTjeneste;

    ArbeidsgiverinitiertDialogRest() {
        // CDI
    }

    @Inject
    public ArbeidsgiverinitiertDialogRest(InntektsmeldingTjeneste inntektsmeldingTjeneste) {
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
    }

    @POST
    @Path(HENT_ARBEIDSFORHOLD)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsforhold(@Valid @NotNull OpplysningerRequestDto request) {
        LOG.info("Henter opplysninger for søker {}", request.fødselsnummer());
        var dto = inntektsmeldingTjeneste.finnArbeidsforholdForFnr(request.fødselsnummer(), request.ytelseType());
        return dto.map(d ->Response.ok(d).build()).orElseGet(() ->Response.noContent().build());
    }
}
