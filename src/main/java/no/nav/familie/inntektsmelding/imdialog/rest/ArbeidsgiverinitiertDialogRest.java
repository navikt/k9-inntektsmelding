package no.nav.familie.inntektsmelding.imdialog.rest;

import java.time.LocalDate;
import java.util.List;

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

import no.nav.familie.inntektsmelding.imdialog.tjenester.GrunnlagTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.FagsakInfo;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.K9SakTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.exception.FunksjonellException;

@AutentisertMedTokenX
@RequestScoped
@Transactional
@Consumes(MediaType.APPLICATION_JSON)
@Path(ArbeidsgiverinitiertDialogRest.BASE_PATH)
public class ArbeidsgiverinitiertDialogRest {
    private static final Logger LOG = LoggerFactory.getLogger(ArbeidsgiverinitiertDialogRest.class);

    public static final String BASE_PATH = "/arbeidsgiverinitiert";
    private static final String HENT_ARBEIDSFORHOLD = "/arbeidsforhold";
    private static final String HENT_OPPLYSNINGER = "/opplysninger";
    private static final String HENT_ARBEIDSGIVERE_UREGISTRERT = "/arbeidsgivere/uregistrert";
    private static final String HENT_OPPLYSNINGER_UREGISTRERT = "/opplysninger/uregistrert";


    private GrunnlagTjeneste grunnlagTjeneste;
    private PersonTjeneste personTjeneste;
    private K9SakTjeneste k9SakTjeneste;

    ArbeidsgiverinitiertDialogRest() {
        // CDI
    }

    @Inject
    public ArbeidsgiverinitiertDialogRest(GrunnlagTjeneste grunnlagTjeneste, PersonTjeneste personTjeneste, K9SakTjeneste k9SakTjeneste) {
        this.grunnlagTjeneste = grunnlagTjeneste;
        this.personTjeneste = personTjeneste;
        this.k9SakTjeneste = k9SakTjeneste;
    }

    @POST
    @Path(HENT_ARBEIDSFORHOLD)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsforhold(@Valid @NotNull HentArbeidsforholdRequest request) {
        LOG.info("Henter arbeidsforhold for søker");

        // Sjekk at person finnes
        PersonInfo personInfo = personTjeneste.hentPersonFraIdent(request.fødselsnummer());
        if (personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        validerSakIK9(personInfo, request.ytelseType(), request.førsteFraværsdag());

        var response = grunnlagTjeneste.finnArbeidsforholdForFnr(request.fødselsnummer(), request.førsteFraværsdag());
        return response.map(d ->Response.ok(d).build()).orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path(HENT_OPPLYSNINGER)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysninger(@Valid @NotNull OpplysningerRequestDto request) {
        LOG.info("Henter opplysninger for søker");
        Ytelsetype ytelsetype = KodeverkMapper.mapYtelsetype(request.ytelseType());
        var hentOpplysningerResponse = grunnlagTjeneste.hentOpplysninger(request.fødselsnummer(), ytelsetype, request.førsteFraværsdag(), request.organisasjonsnummer(), ForespørselType.ARBEIDSGIVERINITIERT_NYANSATT);
        return Response.ok(hentOpplysningerResponse).build();
    }

    @POST
    @Path(HENT_ARBEIDSGIVERE_UREGISTRERT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentArbeidsgivereforUregistrert(@Valid @NotNull HentArbeidsgivereUregistrertRequest request) {
        LOG.info("Henter personinformasjon, og organisasjoner som innsender har tilgang til");
        PersonInfo personInfo = personTjeneste.hentPersonFraIdent(request.fødselsnummer());
        if (personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        var dto = grunnlagTjeneste.hentSøkerinfoOgOrganisasjonerArbeidsgiverHarTilgangTil(personInfo);
        return dto.map(d -> Response.ok(d).build()).orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Path(HENT_OPPLYSNINGER_UREGISTRERT)
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Tilgangskontrollert
    public Response hentOpplysningerUregistrert(@Valid @NotNull OpplysningerRequestDto request) {
        LOG.info("Henter opplysninger for uregistrert søker");

        PersonInfo personInfo = personTjeneste.hentPersonFraIdent(request.fødselsnummer());
        if (personInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        validerSakIK9(personInfo, request.ytelseType(), request.førsteFraværsdag());
        validerAtOrgnummerIkkeFinnesIAaregPåPerson(request, personInfo);

        Ytelsetype ytelsetype = KodeverkMapper.mapYtelsetype(request.ytelseType());
        var hentOpplysningerResponse = grunnlagTjeneste.hentOpplysninger(request.fødselsnummer(), ytelsetype, request.førsteFraværsdag(), request.organisasjonsnummer(), ForespørselType.ARBEIDSGIVERINITIERT_UREGISTRERT);
        return Response.ok(hentOpplysningerResponse).build();
    }

    // TODO: burde denne trekkes ut til grunnlag-tjenesten?
    private void validerSakIK9(PersonInfo personInfo, YtelseTypeDto ytelseType, LocalDate førsteFraværsdag) {
        // Sjekk at søker har sak i k9-sak
        Ytelsetype ytelsetype = KodeverkMapper.mapYtelsetype(ytelseType);
        AktørId aktørId = new AktørId(personInfo.aktørId().getAktørId());
        List<FagsakInfo> fagsakerIK9Sak =  k9SakTjeneste.hentFagsakInfo(ytelsetype, aktørId);
        List<PeriodeDto> søknadsPerioderForFagsakerIK9 = fagsakerIK9Sak.stream()
            .flatMap(fagsak -> fagsak.søknadsPerioder().stream())
            .toList();

        var finnesSakIK9 = søknadsPerioderForFagsakerIK9.stream()
            .anyMatch(søknandsperiode -> søknandsperiode.inneholderDato(førsteFraværsdag));

        if (!finnesSakIK9) {
            var feilmelding = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen", ytelsetype);
            throw new FunksjonellException("INGEN_SAK_FUNNET", feilmelding, null, null);
        }

        if (fagsakerIK9Sak.stream().anyMatch(FagsakInfo::venterForTidligSøknad)) {
            var feilmelding = String.format("Du kan ikke sende inn inntektsmelding før fire uker før denne personen starter med %s", ytelsetype);
            throw new FunksjonellException("SENDT_FOR_TIDLIG", feilmelding, null, null);
        }
    }

    //Er denne sjekken i det hele tatt er nødvendig?
    private void validerAtOrgnummerIkkeFinnesIAaregPåPerson(OpplysningerRequestDto request, PersonInfo personInfo) {
        var finnesOrgnummerIAaReg = grunnlagTjeneste.finnesOrgnummerIAaregPåPerson(personInfo.fødselsnummer(), request.organisasjonsnummer().orgnr(), request.førsteFraværsdag());
        if (finnesOrgnummerIAaReg) {
            var tekst = "Det finnes rapportering i aa-registeret på organisasjonsnummeret. Nav vil be om inntektsmelding når vi trenger det";
            throw new FunksjonellException("FINNES_I_AAREG", tekst, null, null);
        }
    }
}
