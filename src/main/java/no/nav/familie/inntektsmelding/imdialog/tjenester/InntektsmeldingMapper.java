package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.List;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.NaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

public class InntektsmeldingMapper {

    public static InntektsmeldingEntitet mapTilEntitet(SendInntektsmeldingRequestDto dto) {
        return new InntektsmeldingEntitet.InntektsmeldingEntitetBuilder()
            .medAktørId(new AktørIdEntitet(dto.aktorId().id()))
            .medArbeidsgiverIdent(dto.arbeidsgiverIdent().ident())
            .medMånedInntekt(dto.inntekt())
            .medStartDato(dto.startdato())
            .medYtelsetype(KodeverkMapper.mapYtelsetypeTilEntitet(dto.ytelse()))
            .medKontaktperson(mapKontaktPerson(dto))
            .medNaturalYtelse(mapNaturalytelser(dto.bortfaltNaturaltytelsePerioder()))
            .medRefusjonsPeriode(mapRefusjonsperioder(dto.refusjonsperioder()))
            .build();
    }

    private static List<RefusjonPeriodeEntitet> mapRefusjonsperioder(List<SendInntektsmeldingRequestDto.RefusjonsperiodeRequestDto> dto) {
        return dto.stream().map(d -> new RefusjonPeriodeEntitet(d.fom(), d.tom(), d.beløp())).toList();
    }

    private static List<NaturalytelseEntitet> mapNaturalytelser(List<SendInntektsmeldingRequestDto.NaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new NaturalytelseEntitet.Builder()
                .medPeriode(d.fom(), d.tom())
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
