package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.k9.inntektsmelding.felles.FeilkodeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.HentInntektsmeldingerResponse;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingResponse;

@AutentisertMedAzure
@ApplicationScoped
@Transactional
@Path(InntektsmeldingApiRest.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InntektsmeldingApiRest {
    public static final String BASE_PATH = "/imapi/inntektsmelding";
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingApiRest.class);

    private InntektsmeldingApiTjeneste inntektsmeldingApiTjeneste;
    private InntektsmeldingApiMottakTjeneste mottakTjeneste;
    private PersonTjeneste personTjeneste;
    private Tilgang tilgang;

    InntektsmeldingApiRest() {
        // CDI
    }

    @Inject
    public InntektsmeldingApiRest(InntektsmeldingApiTjeneste inntektsmeldingApiTjeneste,
                                  InntektsmeldingApiMottakTjeneste mottakTjeneste,
                                  PersonTjeneste personTjeneste,
                                  Tilgang tilgang) {
        this.inntektsmeldingApiTjeneste = inntektsmeldingApiTjeneste;
        this.mottakTjeneste = mottakTjeneste;
        this.personTjeneste = personTjeneste;
        this.tilgang = tilgang;
    }

    @GET
    @Path("/hent/{inntektsmeldingUuid}")
    @Tilgangskontrollert
    public Response hentInntektsmelding(@NotNull @Valid @PathParam("inntektsmeldingUuid") UUID inntektsmeldingUuid) {
        sjekkErSystemkall();
        Optional<InntektsmeldingDto> im = inntektsmeldingApiTjeneste.hentInntektsmelding(inntektsmeldingUuid);
        if (im.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(im.get()).build();
    }

    @POST
    @Path("/hent/inntektsmeldinger")
    @Tilgangskontrollert
    public Response hentInntektsmeldinger(@NotNull @Valid HentInntektsmeldingerRequest request) {
        sjekkErSystemkall();
        List<InntektsmeldingDto> inntektsmeldinger = inntektsmeldingApiTjeneste.hentInntektsmeldinger(request);
        return Response.ok(new HentInntektsmeldingerResponse(inntektsmeldinger)).build();
    }

    @POST
    @Path("/send-inntektsmelding")
    @Tilgangskontrollert
    public SendInntektsmeldingResponse sendInntektsmelding(@NotNull @Valid SendInntektsmeldingRequest request) {
        sjekkErSystemkall();
        var aktørId = personTjeneste.finnAktørIdForPersonIdent(request.fødselsnummer().fnr());

        if (aktørId.isEmpty()) {
            LOG.error("Finner ikke aktørId for fødselsnummer.");
            return new SendInntektsmeldingResponse(false, null,
                new SendInntektsmeldingResponse.FeilInfo(FeilkodeDto.INGEN_AKTØR_ID,
                    "Finner ikke informasjon for fødselsnummer. Sjekk at fødselsnummer er korrekt",
                    request.foresporselUuid().toString()));
        }
        return mottakTjeneste.mottaInntektsmelding(request, aktørId.get());
    }

    private void sjekkErSystemkall() {
        tilgang.sjekkErSystembruker();
    }
}
