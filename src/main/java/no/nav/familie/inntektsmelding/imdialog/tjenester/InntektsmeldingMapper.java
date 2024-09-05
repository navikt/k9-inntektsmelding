package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingMapper {

    public static InntektsmeldingEntitet mapTilEntitet(SendInntektsmeldingRequestDto dto) {
        return InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(dto.aktorId().id()))
            .medArbeidsgiverIdent(dto.arbeidsgiverIdent().ident())
            .medMånedInntekt(dto.inntekt())
            .medMånedRefusjon(dto.refusjon())
            .medRefusjonOpphørsdato(finnOpphørsdato(dto.refusjonsendringer()).orElse(Tid.TIDENES_ENDE)) // TODO Foretrekker vi null eller tidenes ende?
            .medStartDato(dto.startdato())
            .medYtelsetype(KodeverkMapper.mapYtelsetype(dto.ytelse()))
            .medKontaktperson(mapKontaktPerson(dto))
            .medBortfaltNaturalytelser(mapBortfalteNaturalytelser(dto.bortfaltNaturalytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(dto.refusjonsendringer()))
            .build();
    }

    public static SendInntektsmeldingRequestDto mapFraEntitet(InntektsmeldingEntitet entitet, UUID forespørselUuid) {
        var refusjonsendringer = entitet.getRefusjonsendringer().stream().map(i ->
            new SendInntektsmeldingRequestDto.RefusjonendringRequestDto(i.getFom(), i.getRefusjonPrMnd())
        ).toList();

        var bortfalteNaturalytelser = entitet.getBorfalteNaturalYtelser().stream().map(i ->
            new SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto(
                i.getPeriode().getFom(),
                i.getPeriode().getTom(),
                NaturalytelsetypeDto.valueOf(i.getType().toString()),
                i.getMånedBeløp()
            )
        ).toList();

        return new SendInntektsmeldingRequestDto(
            forespørselUuid,
            new AktørIdDto(entitet.getAktørId().getAktørId()),
             YtelseTypeDto.valueOf(entitet.getYtelsetype().toString()),
            new ArbeidsgiverDto(entitet.getArbeidsgiverIdent()),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto(entitet.getKontaktperson().getNavn(), entitet.getKontaktperson().getTelefonnummer()),
            entitet.getStartDato(),
            entitet.getMånedInntekt(),
            entitet.getMånedRefusjon(),
            refusjonsendringer,
            bortfalteNaturalytelser
            );
    }

    private static Optional<LocalDate> finnOpphørsdato(List<SendInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonsendringRequestDtos) {
        var sisteEndring = refusjonsendringRequestDtos.stream().max(Comparator.comparing(SendInntektsmeldingRequestDto.RefusjonendringRequestDto::fom));
        // Hvis siste endring setter refusjon til 0 er det å regne som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(SendInntektsmeldingRequestDto.RefusjonendringRequestDto::fom);
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(List<SendInntektsmeldingRequestDto.RefusjonendringRequestDto> refusjonsendringRequestDtos) {
        return refusjonsendringRequestDtos.stream().map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beløp())).toList();
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(List<SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto> dto) {
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
