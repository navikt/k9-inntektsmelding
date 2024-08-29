package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.NaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonEndringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingMapper {

    public static InntektsmeldingEntitet mapTilEntitet(SendInntektsmeldingRequestDto dto) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(dto.aktorId().id()))
            .medArbeidsgiverIdent(dto.arbeidsgiverIdent().ident())
            .medMånedInntekt(dto.inntekt())
            .medMånedRefusjon(dto.refusjon())
            .medRefusjonOpphørsdato(finnOpphørsdato(dto.refusjonEndringer()).orElse(Tid.TIDENES_ENDE)) // TODO Foretrekker vi null eller tidenes ende?
            .medStartDato(dto.startdato())
            .medYtelsetype(KodeverkMapper.mapYtelsetype(dto.ytelse()))
            .medKontaktperson(mapKontaktPerson(dto))
            .medNaturalYtelse(mapNaturalytelser(dto.bortfaltNaturaltytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(dto.refusjonEndringer()))
            .build();
    }

    private static Optional<LocalDate> finnOpphørsdato(List<SendInntektsmeldingRequestDto.RefusjonEndringRequestDto> refusjonEndringRequestDtos) {
        var sisteEndring = refusjonEndringRequestDtos.stream().max(Comparator.comparing(SendInntektsmeldingRequestDto.RefusjonEndringRequestDto::fom));
        // Hvis siste endring setter refusjon til 0 er det å regne som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(SendInntektsmeldingRequestDto.RefusjonEndringRequestDto::fom);
    }

    private static List<RefusjonEndringEntitet> mapRefusjonsendringer(List<SendInntektsmeldingRequestDto.RefusjonEndringRequestDto> refusjonEndringRequestDtos) {
        return refusjonEndringRequestDtos.stream().map(dto -> new RefusjonEndringEntitet(dto.fom(), dto.beløp())).toList();
    }

    private static List<NaturalytelseEntitet> mapNaturalytelser(List<SendInntektsmeldingRequestDto.NaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new NaturalytelseEntitet.Builder().medPeriode(d.fom(), d.tom())
                .medBeløp(d.beløp())
                .medType(KodeverkMapper.mapNaturalytelseTilEntitet(d.naturalytelsetype()))
                .medErBortfalt(d.erBortfalt())
                .build())
            .toList();
    }

    private static KontaktpersonEntitet mapKontaktPerson(SendInntektsmeldingRequestDto dto) {
        return new KontaktpersonEntitet(dto.kontaktperson().navn(), dto.kontaktperson().telefonnummer());
    }
}
