package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
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
            .medBortfaltNaturalytelser(mapBortfalteNaturalytelser(dto.bortfaltNaturalytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(dto.refusjonEndringer()))
            .build();
    }

    private static Optional<LocalDate> finnOpphørsdato(List<SendInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonEndringRequestDtos) {
        var sisteEndring = refusjonEndringRequestDtos.stream().max(Comparator.comparing(SendInntektsmeldingRequestDto.RefusjonendringRequestDto::fom));
        // Hvis siste endring setter refusjon til 0 er det å regne som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(SendInntektsmeldingRequestDto.RefusjonendringRequestDto::fom);
    }

    private static List<RefusjonEndringEntitet> mapRefusjonsendringer(List<SendInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonEndringRequestDtos) {
        return refusjonEndringRequestDtos.stream().map(dto -> new RefusjonEndringEntitet(dto.fom(), dto.beløp())).toList();
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(List<SendInntektsmeldingRequestDto.NaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new BortaltNaturalytelseEntitet.Builder().medPeriode(d.fom(), d.tom() != null ? d.tom() : Tid.TIDENES_ENDE )
                .medMånedBeløp(d.beløp())
                .medType(KodeverkMapper.mapNaturalytelseTilEntitet(d.naturalytelsetype()))
                .build())
            .toList();
    }

    private static KontaktpersonEntitet mapKontaktPerson(SendInntektsmeldingRequestDto dto) {
        return new KontaktpersonEntitet(dto.kontaktperson().navn(), dto.kontaktperson().telefonnummer());
    }
}
