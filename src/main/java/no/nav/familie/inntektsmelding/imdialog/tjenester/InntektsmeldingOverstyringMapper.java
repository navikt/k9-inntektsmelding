package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.SendOverstyrtInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingOverstyringMapper {

    public static InntektsmeldingEntitet mapTilEntitet(SendOverstyrtInntektsmeldingRequestDto dto) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(dto.aktorId().id()))
            .medArbeidsgiverIdent(dto.arbeidsgiverIdent().ident())
            .medKildesystem(Kildesystem.FPSAK)
            .medOpprettetAv(dto.opprettetAv())
            .medMånedInntekt(dto.inntekt())
            .medMånedRefusjon(dto.refusjon())
            .medRefusjonOpphørsdato(finnOpphørsdato(dto.refusjonsendringer()).orElse(Tid.TIDENES_ENDE))
            .medStartDato(dto.startdato())
            .medYtelsetype(KodeverkMapper.mapYtelsetype(dto.ytelse()))
            .medBortfaltNaturalytelser(mapBortfalteNaturalytelser(dto.bortfaltNaturalytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(dto.refusjonsendringer()))
            .build();
    }

    private static Optional<LocalDate> finnOpphørsdato(
        List<SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonsendringRequestDtos) {
        var sisteEndring = refusjonsendringRequestDtos.stream().max(Comparator.comparing(SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto::fom));
        // Hvis siste endring setter refusjon til 0 er det å regne som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto::fom);
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(
        List<SendOverstyrtInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonsendringRequestDtos) {
        return refusjonsendringRequestDtos.stream().map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beløp())).toList();
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(
        List<SendOverstyrtInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new BortaltNaturalytelseEntitet.Builder().medPeriode(d.fom(), d.tom() != null ? d.tom() : Tid.TIDENES_ENDE )
                .medMånedBeløp(d.beløp())
                .medType(KodeverkMapper.mapNaturalytelseTilEntitet(d.naturalytelsetype()))
                .build())
            .toList();
    }
}
