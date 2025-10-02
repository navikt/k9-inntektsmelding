package no.nav.familie.inntektsmelding.forvaltning;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper.mapEndringsårsak;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.K9DokgenTjeneste;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.Tilgangskontrollert;
import no.nav.familie.inntektsmelding.server.tilgangsstyring.Tilgang;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.EndringsårsakDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.konfig.Tid;

@ApplicationScoped
@Path(K9DokgenRestTjeneste.BASE_PATH)
@Produces(MediaType.APPLICATION_JSON)
@AutentisertMedAzure
/*Denne tjenesten er ment brukt til testformål, og eventuelt for å gjenskape feilsituasjoner i produksjon*/
public class K9DokgenRestTjeneste {
    public static final String BASE_PATH = "/inntektsmelding-pdf";
    private static final Logger LOG = LoggerFactory.getLogger(K9DokgenRestTjeneste.class);
    private static final boolean IS_PROD = Environment.current().isProd();
    private K9DokgenTjeneste k9DokgenTjeneste;
    private Tilgang tilgang;

    private InntektsmeldingRepository inntektsmeldingRepository;

    public K9DokgenRestTjeneste() {
        //CDI
    }

    @Inject
    public K9DokgenRestTjeneste(K9DokgenTjeneste k9DokgenTjeneste, Tilgang tilgang, InntektsmeldingRepository inntektsmeldingRepository) {
        this.k9DokgenTjeneste = k9DokgenTjeneste;
        this.tilgang = tilgang;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    @POST
    @Consumes(APPLICATION_JSON)
    @Produces("application/pdf")
    @Operation(description = "Generer en pdf av en inntektsmelding", tags = "forvaltning")
    @Tilgangskontrollert
    public Response genererPdf(@Valid @NotNull InntektsmeldingRequest inntektsmeldingRequest) {
        if (IS_PROD) {
            throw new ManglerTilgangException("IKKE-TILGANG", "Ikke tilgjengelig i produksjon");
        }
        sjekkAtKallerHarRollenDrift();

        InntektsmeldingEntitet inntektsmeldingEntitet;
        if (inntektsmeldingRequest.inntektsmeldingId != null) {
            inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingRequest.inntektsmeldingId.intValue());
            LOG.info("Generer en pdf av en inntektsmelding med id: {}", inntektsmeldingRequest.inntektsmeldingId);
        } else {
            //For testformål
            var builder = InntektsmeldingEntitet.builder()
                .medAktørId(new AktørIdEntitet(inntektsmeldingRequest.aktørId.id()))
                .medKontaktperson(new KontaktpersonEntitet(inntektsmeldingRequest.kontaktpersonNavn, inntektsmeldingRequest.kontaktpersonTlf))
                .medMånedInntekt(inntektsmeldingRequest.maanedInntekt())
                .medYtelsetype(mapYtelseType(inntektsmeldingRequest.ytelsetype()))
                .medInntektsmeldingType(InntektsmeldingType.ORDINÆR)
                .medOpprettetTidspunkt(LocalDateTime.now())
                .medMånedRefusjon(inntektsmeldingRequest.maanedRefusjon())
                .medStartDato(inntektsmeldingRequest.startdatoPermisjon())
                .medArbeidsgiverIdent(inntektsmeldingRequest.arbeidsgiverIdent())
                .medEndringsårsaker(mapEndringsårsker(inntektsmeldingRequest.endringsårsaker));

            if (inntektsmeldingRequest.opphoersdatoRefusjon() != null) {
                builder.medRefusjonOpphørsdato(inntektsmeldingRequest.opphoersdatoRefusjon());
            } else {
                builder.medRefusjonOpphørsdato(Tid.TIDENES_ENDE);
            }

            if (inntektsmeldingRequest.refusjonsendringer() != null) {
                builder.medRefusjonsendringer(mapRefusjonsendringer(inntektsmeldingRequest.refusjonsendringer()));
            }
            if (inntektsmeldingRequest.naturalytelser() != null) {
                builder.medBortfaltNaturalytelser(mapBortfalteNaturalytelser(inntektsmeldingRequest.naturalytelser));
            }

            if (inntektsmeldingRequest.omsorgspenger() != null) {
                builder.medOmsorgspenger(mapOmsorgspenger(inntektsmeldingRequest.omsorgspenger()));
            }

            inntektsmeldingEntitet = builder.build();
        }

        var pdf = k9DokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet);

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=dokument.pdf");
        return responseBuilder.build();
    }

    private List<EndringsårsakEntitet> mapEndringsårsker(List<EndringsårsakerDto> endringsårsaker) {
        return endringsårsaker.stream().map(endringsårsak -> new EndringsårsakEntitet.Builder().
                medÅrsak(mapEndringsårsak(endringsårsak.aarsak()))
                .medFom(endringsårsak.fom())
                .medTom(endringsårsak.tom())
                .medBleKjentFra(endringsårsak.bleKjentFom())
                .build())
            .toList();
    }

    private List<RefusjonsendringEntitet> mapRefusjonsendringer(List<EndringRefusjonDto> refusjonsendringer) {
        return refusjonsendringer.stream().
            map(periode -> new RefusjonsendringEntitet(periode.fom(), periode.beloep()))
            .toList();
    }


    private List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(List<BortfaltNaturalytelseDto> naturalYtelser) {
        return naturalYtelser.stream()
            .map(periode -> new BortaltNaturalytelseEntitet.Builder().medPeriode(periode.fom(), periode.tom())
                .medType(periode.type())
                .medMånedBeløp(periode.maanedBortfaltNaturalytelse())
                .build())
            .toList();
    }

    private static OmsorgspengerEntitet mapOmsorgspenger(OmsorgspengerRequestDto dto) {
        return OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(dto.harUtbetaltPliktigeDager())
            .medFraværsPerioder(mapFraværsPerioder(dto.fraværsPerioder()))
            .medDelvisFraværsPerioder(mapDelvisFraværsPerioder(dto.delvisFraværsPerioder()))
            .build();
    }

    private static List<FraværsPeriodeEntitet> mapFraværsPerioder(List<FraværsPeriodeRequestDto> dto) {
        if (dto == null) {
            return null;
        }
        return dto.stream()
            .map(fraværsPeriode -> new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(fraværsPeriode.fom(), fraværsPeriode.tom())))
            .toList();
    }

    private static List<DelvisFraværsPeriodeEntitet> mapDelvisFraværsPerioder(List<DelvisFraværsPeriodeRequestDto> dto) {
        if (dto == null) {
            return null;
        }
        return dto.stream()
            .map(delvisFraværsPeriode -> new DelvisFraværsPeriodeEntitet(delvisFraværsPeriode.dato(), delvisFraværsPeriode.timer()))
            .toList();
    }

    private Ytelsetype mapYtelseType(String ytelsetype) {
        return switch (ytelsetype.toLowerCase()) {
            case "pleiepenger sykt barn" -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case "pleiepenger i livets sluttfase" -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
            case "opplæringspenger" -> Ytelsetype.OPPLÆRINGSPENGER;
            case "omsorgspenger" -> Ytelsetype.OMSORGSPENGER;
            default -> throw new IllegalArgumentException("Ugyldig ytelsetype: " + ytelsetype);
        };
    }

    public record InntektsmeldingRequest(Long inntektsmeldingId,
                                         AktørIdDto aktørId,
                                         String ytelsetype,
                                         String arbeidsgiverIdent,
                                         String kontaktpersonNavn,
                                         String kontaktpersonTlf,
                                         LocalDate startdatoPermisjon,
                                         LocalDate opphoersdatoRefusjon,
                                         @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal maanedRefusjon,
                                         @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal maanedInntekt,
                                         List<EndringRefusjonDto> refusjonsendringer,
                                         List<BortfaltNaturalytelseDto> naturalytelser,
                                         List<EndringsårsakerDto> endringsårsaker,
                                         OmsorgspengerRequestDto omsorgspenger
                                         ) {
    }

    public record EndringRefusjonDto(@NotNull LocalDate fom,
                                     @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal beloep) {
    }

    public record BortfaltNaturalytelseDto(@NotNull LocalDate fom,
                                           LocalDate tom,
                                           NaturalytelseType type,
                                           @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 20, fraction = 2) BigDecimal maanedBortfaltNaturalytelse) {
    }

    public record EndringsårsakerDto(@NotNull EndringsårsakDto aarsak,
                                     LocalDate fom,
                                     LocalDate tom,
                                     LocalDate bleKjentFom) {
    }


    public record OmsorgspengerRequestDto(@NotNull Boolean harUtbetaltPliktigeDager,
                                          List<@Valid FraværsPeriodeRequestDto> fraværsPerioder,
                                          List<@Valid DelvisFraværsPeriodeRequestDto> delvisFraværsPerioder) {
    }

    public record FraværsPeriodeRequestDto(@NotNull LocalDate fom,
                                           @NotNull LocalDate tom) {

    }

    public record DelvisFraværsPeriodeRequestDto(@NotNull LocalDate dato,
                                                 @NotNull @Min(0) @Max(Integer.MAX_VALUE) @Digits(integer = 2, fraction = 2) BigDecimal timer) {
    }

    private void sjekkAtKallerHarRollenDrift() {
        tilgang.sjekkAtAnsattHarRollenDrift();
    }
}
