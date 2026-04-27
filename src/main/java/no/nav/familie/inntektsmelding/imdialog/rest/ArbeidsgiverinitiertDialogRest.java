package no.nav.familie.inntektsmelding.imdialog.rest;

import static no.nav.familie.inntektsmelding.imdialog.tjenester.ArbeidsgiverinitiertDialogRestValiderer.SøknadsperiodeValidering.FRAVÆRSDAG_ER_FØRSTE_FRAVÆRSDAG_I_SØKNADSPERIODE;
import static no.nav.familie.inntektsmelding.imdialog.tjenester.ArbeidsgiverinitiertDialogRestValiderer.SøknadsperiodeValidering.FRAVÆRSDAG_INNENFOR_SØKNADSPERIODE;

import java.util.Optional;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.tjenester.ArbeidsgiverinitiertDialogRestValiderer;
import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(ArbeidsgiverinitiertDialogRest.BASE_PATH)
public class ArbeidsgiverinitiertDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverinitiertDialogRest.class);

    public static final String BASE_PATH = "/arbeidsgiverinitiert";
    private static final String HENT_ARBEIDSFORHOLD_NYANSATT = "/arbeidsforhold/nyansatt";
    private static final String HENT_OPPLYSNINGER_NYANSATT = "/opplysninger/nyansatt";
    private static final String HENT_ARBEIDSGIVERE_UREGISTRERT = "/arbeidsgivere/uregistrert";
    private static final String HENT_ARBEIDSGIVER_ORGANISASJONER = "/arbeidsgiver/organisasjoner";
    private static final String HENT_OPPLYSNINGER_UREGISTRERT = "/opplysninger/uregistrert";
    private static final String HENT_ARBEIDSTAKER = "/arbeidstaker";

    private GrunnlagTjeneste grunnlagTjeneste;
    private PersonTjeneste personTjeneste;
    private ArbeidsgiverinitiertDialogRestValiderer arbeidsgiverinitiertDialogRestValiderer;

    ArbeidsgiverinitiertDialogRest() {
        // CDI
    }

    @Inject
    public ArbeidsgiverinitiertDialogRest(GrunnlagTjeneste grunnlagTjeneste,
                                          PersonTjeneste personTjeneste,
                                          ArbeidsgiverinitiertDialogRestValiderer arbeidsgiverinitiertDialogRestValiderer) {
        this.grunnlagTjeneste = grunnlagTjeneste;
        this.personTjeneste = personTjeneste;
        this.arbeidsgiverinitiertDialogRestValiderer = arbeidsgiverinitiertDialogRestValiderer;
    }

    @POST
    @Path(HENT_ARBEIDSFORHOLD_NYANSATT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsforholdNyansatt(@Valid @NotNull HentArbeidsforholdRequest request) {
        LOG.info("Henter arbeidsforhold for søker");
        PersonInfo personInfo = personTjeneste.hentPersonFraIdent(request.fødselsnummer());
        arbeidsgiverinitiertDialogRestValiderer.validerPerson(personInfo);
        arbeidsgiverinitiertDialogRestValiderer.validerSakIK9(personInfo,
            request.ytelseType(),
            request.førsteFraværsdag(),
            FRAVÆRSDAG_INNENFOR_SØKNADSPERIODE);

        Optional<HentArbeidsforholdResponse> response = grunnlagTjeneste.finnArbeidsforholdForFnr(personInfo, request.førsteFraværsdag());
        arbeidsgiverinitiertDialogRestValiderer.validerArbeidsforhold(response);
        return response.map(r -> Response.ok(r).build()).orElseThrow(() -> new RuntimeException("Arbeidsforhold skal være valid"));
    }

    @POST
    @Path(HENT_OPPLYSNINGER_NYANSATT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysningerNyansatt(@Valid @NotNull OpplysningerRequestDto request) {
        LOG.info("Henter opplysninger for søker");
        Ytelsetype ytelsetype = KodeverkMapper.mapYtelsetype(request.ytelseType());
        HentOpplysningerResponse hentOpplysningerResponse = grunnlagTjeneste.hentOpplysninger(request.fødselsnummer(),
            ytelsetype,
            request.førsteFraværsdag(),
            request.organisasjonsnummer(),
            ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        return Response.ok(hentOpplysningerResponse).build();
    }

    /**
     * @deprecated Bruk {@link #hentArbeidstaker(HentArbeidstakerRequest)} i stedet.
     */
    @Deprecated
    @POST
    @Path(HENT_ARBEIDSGIVERE_UREGISTRERT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsgivereUregistrert(@Valid @NotNull HentArbeidsforholdRequest request) {
        LOG.info("Henter personinformasjon, og organisasjoner som innsender har tilgang til");
        PersonInfo personInfo = personTjeneste.hentPersonFraIdent(request.fødselsnummer());

        arbeidsgiverinitiertDialogRestValiderer.validerPerson(personInfo);
        arbeidsgiverinitiertDialogRestValiderer.validerSakIK9(personInfo,
            request.ytelseType(),
            request.førsteFraværsdag(),
            FRAVÆRSDAG_ER_FØRSTE_FRAVÆRSDAG_I_SØKNADSPERIODE);

        // siden arbeidstager er uregistrert slår vi opp organisasjoner arbeidsgiver har tilgang til
        var organisasjonerArbeidsgiverHarTilgangTil = grunnlagTjeneste.hentOrganisasjonerSomArbeidsgiverHarTilgangTil();
        HentArbeidsforholdResponse response = grunnlagTjeneste.lagHentArbeidsforholdResponse(personInfo, organisasjonerArbeidsgiverHarTilgangTil);
        arbeidsgiverinitiertDialogRestValiderer.validerArbeidsforhold(response);
        return Response.ok(response).build();
    }

    @POST
    @Path(HENT_ARBEIDSTAKER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidstaker(@Valid @NotNull HentArbeidstakerRequest request) {
        LOG.info("Henter arbeidstaker");
        PersonInfo personInfo = personTjeneste.hentPersonFraIdent(request.fødselsnummer());
        arbeidsgiverinitiertDialogRestValiderer.validerPerson(personInfo);

        var response = new HentArbeidstakerResponse(personInfo.fornavn(),
            personInfo.mellomnavn(),
            personInfo.etternavn(),
            personInfo.kjønn(),
            personInfo.aktørId().getAktørId());
        return Response.ok(response).build();
    }

    @POST
    @Path(HENT_OPPLYSNINGER_UREGISTRERT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysningerUregistrert(@Valid @NotNull OpplysningerRequestDto request) {
        LOG.info("Henter opplysninger for uregistrert søker");
        PersonInfo personInfo = personTjeneste.hentPersonFraIdent(request.fødselsnummer());

        arbeidsgiverinitiertDialogRestValiderer.validerPerson(personInfo);
        arbeidsgiverinitiertDialogRestValiderer.validerSakIK9(personInfo,
            request.ytelseType(),
            request.førsteFraværsdag(),
            FRAVÆRSDAG_ER_FØRSTE_FRAVÆRSDAG_I_SØKNADSPERIODE);
        arbeidsgiverinitiertDialogRestValiderer.validerAtOrgnummerIkkeFinnesIAaregPåPerson(personInfo,
            request.organisasjonsnummer(),
            request.førsteFraværsdag());

        Ytelsetype ytelsetype = KodeverkMapper.mapYtelsetype(request.ytelseType());
        HentOpplysningerResponse response = grunnlagTjeneste.hentOpplysninger(request.fødselsnummer(),
            ytelsetype,
            request.førsteFraværsdag(),
            request.organisasjonsnummer(),
            ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);
        return Response.ok(response).build();
    }

    @GET
    @Path(HENT_ARBEIDSGIVER_ORGANISASJONER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsgiverOrganisasjoner() {
        LOG.info("Henter organisasjoner som arbeidsgiver har tilgang til");
        var organisasjonerArbeidsgiverHarTilgangTil = grunnlagTjeneste.hentOrganisasjonerSomArbeidsgiverHarTilgangTil();
        return Response.ok(new HentArbeidsgiverOrganisasjonerResponse(organisasjonerArbeidsgiverHarTilgangTil)).build();
    }
}
