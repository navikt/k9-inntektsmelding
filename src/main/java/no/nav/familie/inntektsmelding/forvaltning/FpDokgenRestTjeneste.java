package no.nav.familie.inntektsmelding.forvaltning;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.NaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonPeriodeEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.dokgen.FpDokgenTjeneste;
import no.nav.familie.inntektsmelding.koder.Naturalytelsetype;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
@Path(FpDokgenRestTjeneste.BASE_PATH)
public class FpDokgenRestTjeneste {
    public static final String BASE_PATH = "/inntektsmelding-pdf";
    private static final Logger LOG = LoggerFactory.getLogger(FpDokgenTjeneste.class);
    private FpDokgenTjeneste fpDokgenTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;


    public FpDokgenRestTjeneste() {
        //CDI
    }

    public FpDokgenRestTjeneste(FpDokgenTjeneste fpDokgenTjeneste,
                                InntektsmeldingRepository inntektsmeldingRepository) {
        this.fpDokgenTjeneste = fpDokgenTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Generer en pdf av en inntektsmelding", tags = "fpdokgen")
    public Response genererPdf(InntektsmeldingDto inntektsmeldingDto) {
        InntektsmeldingEntitet inntektsmeldingEntitet;
        if (inntektsmeldingDto.inntektsmeldingId != null) {
            //Genererer en pdf på eksisterende inntektsmelding
            inntektsmeldingEntitet = inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingDto.inntektsmeldingId.intValue());
            LOG.info("Generer en pdf av en inntektsmelding med id: {}", inntektsmeldingDto.inntektsmeldingId);
        } else {
            //Basert på innsendte data - test formål
            inntektsmeldingEntitet = InntektsmeldingEntitet.builder()
                .medAktørId(AktørIdEntitet.dummy())
                .medKontaktperson(new KontaktpersonEntitet(inntektsmeldingDto.kontaktpersonNavn, inntektsmeldingDto.kontaktpersonTlf))
                .medMånedInntekt(inntektsmeldingDto.månedInntekt())
                .medYtelsetype(mapYtelseType(inntektsmeldingDto.ytelsetype()))
                .medOpprettetTidspunkt(LocalDateTime.now())
                .medStartDato(inntektsmeldingDto.startdatoPermisjon())
                .medArbeidsgiverIdent(inntektsmeldingDto.arbeidsgiverIdent())
                .medRefusjonsPeriode(mapRefusjonsperiode(inntektsmeldingDto.refusjonsperioder()))
                .build();
        }

        var pdf = fpDokgenTjeneste.mapDataOgGenererPdf(inntektsmeldingEntitet, inntektsmeldingEntitet.getId().intValue());

        var responseBuilder = Response.ok(pdf);
        responseBuilder.type("application/pdf");
        responseBuilder.header("Content-Disposition", "attachment; filename=dokument.pdf");
        return responseBuilder.build();
    }

    private List<RefusjonPeriodeEntitet> mapRefusjonsperiode(List<RefusjonPeriodeDto> refusjonsperioder) {
        return refusjonsperioder.stream().map(periode -> new RefusjonPeriodeEntitet(periode.fom(), periode.tom(), periode.beloep())).toList();
    }

    private List<NaturalytelseEntitet> mapNaturalytelser(List<NaturalYtelseDto> naturalYtelser) {
        return naturalYtelser.stream()
            .map(periode -> new NaturalytelseEntitet.Builder().medPeriode(periode.fom(), periode.tom())
                .medType(Naturalytelsetype.BIL)
                .medBeløp(periode.beloep())
                .medErBortfalt(periode.erBortfalt())
                .build())
            .toList();
    }

    private Ytelsetype mapYtelseType(String ytelsetype) {
        return switch (ytelsetype.toLowerCase()) {
            case "foreldrepenger" -> Ytelsetype.FORELDREPENGER;
            case "svangerskapspenger" -> Ytelsetype.SVANGERSKAPSPENGER;
            case "pleiepenger sykt barn" -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case "pleiepenger nærstående" -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
            case "opplæringspenger" -> Ytelsetype.OPPLÆRINGSPENGER;
            case "omsorgspenger" -> Ytelsetype.OMSORGSPENGER;
            default -> throw new IllegalArgumentException("Ugyldig ytelsetype: " + ytelsetype);
        };
    }

    public record InntektsmeldingDto(Long inntektsmeldingId, @NotNull String ytelsetype, @NotNull String arbeidsgiverIdent,
                                     @NotNull String kontaktpersonNavn, @NotNull String kontaktpersonTlf, @NotNull LocalDate startdatoPermisjon,
                                     @NotNull BigDecimal månedInntekt, List<RefusjonPeriodeDto> refusjonsperioder, List<NaturalYtelseDto> naturalytelser) {
    }

    public record RefusjonPeriodeDto(LocalDate fom, LocalDate tom, BigDecimal beloep ) {
    }

    public record NaturalYtelseDto(LocalDate fom, LocalDate tom, String type, BigDecimal beloep, boolean erBortfalt) {
    }
}
