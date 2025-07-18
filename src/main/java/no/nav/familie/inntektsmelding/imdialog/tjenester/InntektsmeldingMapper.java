package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.DelvisFraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.FraværsPeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.OmsorgspengerEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingResponseDto;
import no.nav.familie.inntektsmelding.imdialog.rest.OmsorgspengerRequestDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.typer.dto.AktørIdDto;
import no.nav.familie.inntektsmelding.typer.dto.ArbeidsgiverDto;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.NaturalytelsetypeDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

public class InntektsmeldingMapper {

    private InntektsmeldingMapper() {
        // Skjuler default konstruktør
    }

    public static InntektsmeldingEntitet mapTilEntitet(SendInntektsmeldingRequestDto dto, ForespørselEntitet forespørsel) {
        // Frontend sender kun inn liste med refusjon. Vi utleder startsum og opphørsdato utifra denne lista.
        var refusjonPrMnd = finnFørsteRefusjon(dto.refusjon(), dto.startdato()).orElse(null);
        var opphørsdato = refusjonPrMnd == null ? null : finnOpphørsdato(dto.refusjon(), dto.startdato()).orElse(Tid.TIDENES_ENDE);
        var inntektsmeldingBuilder = InntektsmeldingEntitet.builder()
            .medAktørId(new AktørIdEntitet(dto.aktorId().id()))
            .medArbeidsgiverIdent(dto.arbeidsgiverIdent().ident())
            .medMånedInntekt(dto.inntekt())
            .medKildesystem(Kildesystem.ARBEIDSGIVERPORTAL)
            .medMånedRefusjon(refusjonPrMnd)
            .medRefusjonOpphørsdato(opphørsdato)
            .medStartDato(dto.startdato())
            .medYtelsetype(KodeverkMapper.mapYtelsetype(dto.ytelse()))
            .medKontaktperson(mapKontaktPerson(dto))
            .medEndringsårsaker(mapEndringsårsaker(dto.endringAvInntektÅrsaker()))
            .medBortfaltNaturalytelser(mapBortfalteNaturalytelser(dto.bortfaltNaturalytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(dto.startdato(), opphørsdato, dto.refusjon()))
            .medForespørsel(forespørsel);

        if (dto.omsorgspenger() != null) { // omsorgspenger refusjon
            inntektsmeldingBuilder.medOmsorgspenger(mapOmsorgspenger(dto.omsorgspenger()));
        }

        if (erDetOmsorgspengerDirekteUtbetaling(dto.ytelse(), forespørsel)) { // omsorgepenger direkte utbetaling
            inntektsmeldingBuilder.medOmsorgspenger(mapOmsorgspengerFraForespørsel(forespørsel.getEtterspurtePerioder()));
        }

        return inntektsmeldingBuilder.build();
    }

    private static boolean erDetOmsorgspengerDirekteUtbetaling(YtelseTypeDto ytelse, ForespørselEntitet forespørsel) {
        return ytelse.equals(YtelseTypeDto.OMSORGSPENGER)
            && forespørsel.getEtterspurtePerioder() != null
            && !forespørsel.getEtterspurtePerioder().isEmpty();
    }

    private static Optional<BigDecimal> finnFørsteRefusjon(List<SendInntektsmeldingRequestDto.Refusjon> refusjon, LocalDate startdato) {
        if (refusjon.isEmpty()) {
            return Optional.empty();
        }
        var refusjonPåStartdato = refusjon.stream().filter(r -> r.fom().equals(startdato)).toList();
        if (refusjonPåStartdato.size() != 1) {
            throw new IllegalStateException("Forventer kun 1 refusjon som starter på startdato, fant " + refusjonPåStartdato.size());
        }
        return Optional.of(refusjonPåStartdato.getFirst().beløp());
    }

    private static List<EndringsårsakEntitet> mapEndringsårsaker(List<SendInntektsmeldingRequestDto.EndringsårsakerRequestDto> endringsårsaker) {
        return endringsårsaker.stream().map(InntektsmeldingMapper::mapEndringsårsak).toList();
    }

    private static EndringsårsakEntitet mapEndringsårsak(SendInntektsmeldingRequestDto.EndringsårsakerRequestDto e) {
        return EndringsårsakEntitet.builder()
            .medÅrsak(KodeverkMapper.mapEndringsårsak(e.årsak()))
            .medFom(e.fom())
            .medTom(e.tom())
            .medBleKjentFra(e.bleKjentFom())
            .build();
    }

    public static InntektsmeldingResponseDto mapFraEntitet(InntektsmeldingEntitet imEntitet, UUID forespørselUuid) {
        var refusjoner = mapRefusjonerTilDto(imEntitet);
        var bortfalteNaturalytelser = mapTilBortfaltNaturalytelseRequestDto(imEntitet);
        var endringsårsaker = mapTilEndringsårsakerRequestDto(imEntitet);

        OmsorgspengerRequestDto omsorgspenger = null;
        if (imEntitet.getOmsorgspenger() != null) {
            omsorgspenger = mapTilOmsorgspengerRequestDto(imEntitet);
        }

        return new InntektsmeldingResponseDto(
            imEntitet.getId(),
            forespørselUuid,
            new AktørIdDto(imEntitet.getAktørId().getAktørId()),
            KodeverkMapper.mapYtelsetype(imEntitet.getYtelsetype()),
            new ArbeidsgiverDto(imEntitet.getArbeidsgiverIdent()),
            new SendInntektsmeldingRequestDto.KontaktpersonRequestDto(imEntitet.getKontaktperson().getNavn(), imEntitet.getKontaktperson().getTelefonnummer()),
            imEntitet.getStartDato(),
            imEntitet.getMånedInntekt(),
            imEntitet.getOpprettetTidspunkt(),
            refusjoner,
            bortfalteNaturalytelser,
            endringsårsaker,
            omsorgspenger
        );
    }

    private static List<SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto> mapTilBortfaltNaturalytelseRequestDto(InntektsmeldingEntitet imEntitet) {
        var bortfalteNaturalytelser = imEntitet.getBorfalteNaturalYtelser()
            .stream()
            .map(bortfaltNaturalytelse ->
                new SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto(
                    bortfaltNaturalytelse.getPeriode().getFom(),
                    Objects.equals(bortfaltNaturalytelse.getPeriode().getTom(), Tid.TIDENES_ENDE) ? null : bortfaltNaturalytelse.getPeriode().getTom(),
                    NaturalytelsetypeDto.valueOf(bortfaltNaturalytelse.getType().toString()),
                    bortfaltNaturalytelse.getMånedBeløp()
                )
            ).toList();
        return bortfalteNaturalytelser;
    }

    private static List<SendInntektsmeldingRequestDto.EndringsårsakerRequestDto> mapTilEndringsårsakerRequestDto(InntektsmeldingEntitet imEntitet) {
        var endringsårsaker = imEntitet.getEndringsårsaker()
            .stream()
            .map(endringsårsak ->
                new SendInntektsmeldingRequestDto.EndringsårsakerRequestDto(KodeverkMapper.mapEndringsårsak(endringsårsak.getÅrsak()),
                    endringsårsak.getFom().orElse(null),
                    endringsårsak.getTom().orElse(null),
                    endringsårsak.getBleKjentFom().orElse(null)))
            .toList();
        return endringsårsaker;
    }

    private static OmsorgspengerRequestDto mapTilOmsorgspengerRequestDto(InntektsmeldingEntitet imEntitet) {
        if (imEntitet.getOmsorgspenger() == null) {
            return null;
        }

        var omsorgspengerEntitet = imEntitet.getOmsorgspenger();
        var omsorgspenger = new OmsorgspengerRequestDto(
            omsorgspengerEntitet.isHarUtbetaltPliktigeDager(),
            omsorgspengerEntitet.getFraværsPerioder()
                .stream()
                .map(fravær -> new OmsorgspengerRequestDto.FraværHeleDagerRequestDto(fravær.getPeriode().getFom(), fravær.getPeriode().getTom()))
                .toList(),
            omsorgspengerEntitet.getDelvisFraværsPerioder()
                .stream()
                .map(delvisFravær -> new OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto(delvisFravær.getDato(), delvisFravær.getTimer()))
                .toList()
        );

        return omsorgspenger;
    }

    private static List<SendInntektsmeldingRequestDto.Refusjon> mapRefusjonerTilDto(InntektsmeldingEntitet entitet) {
        List<SendInntektsmeldingRequestDto.Refusjon> refusjoner = new ArrayList<>();
        if (entitet.getMånedRefusjon() != null) {
            refusjoner.add(new SendInntektsmeldingRequestDto.Refusjon(entitet.getStartDato(), entitet.getMånedRefusjon()));
        }
        // Frontend forventer at opphørsdato mappes til en liste der fom = første dag uten refusjon, må derfor legge på en dag.
        if (entitet.getOpphørsdatoRefusjon() != null && !entitet.getOpphørsdatoRefusjon().equals(Tid.TIDENES_ENDE)) {
            refusjoner.add(new SendInntektsmeldingRequestDto.Refusjon(entitet.getOpphørsdatoRefusjon().plusDays(1), BigDecimal.ZERO));
        }
        entitet.getRefusjonsendringer()
            .stream()
            .map(i -> new SendInntektsmeldingRequestDto.Refusjon(i.getFom(), i.getRefusjonPrMnd()))
            .forEach(refusjoner::add);
        return refusjoner.stream().sorted(Comparator.comparing(SendInntektsmeldingRequestDto.Refusjon::fom)).toList();
    }

    private static Optional<LocalDate> finnOpphørsdato(List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDtos,
                                                       LocalDate startdato) {
        var sisteEndring = finnSisteEndring(refusjonsendringRequestDtos, startdato);
        // Hvis siste endring setter refusjon til 0 er det å regne som opphør av refusjon,
        // setter dagen før denne endringen som opphørsdato
        return sisteEndring.filter(en -> en.beløp().compareTo(BigDecimal.ZERO) == 0).map(sr -> sr.fom().minusDays(1));
    }

    private static Optional<SendInntektsmeldingRequestDto.Refusjon> finnSisteEndring(List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDtos,
                                                                                     LocalDate startdato) {
        return refusjonsendringRequestDtos.stream()
            .filter(r -> !r.fom().equals(startdato))
            .max(Comparator.comparing(SendInntektsmeldingRequestDto.Refusjon::fom));
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(LocalDate startdato,
                                                                       LocalDate opphørsdato,
                                                                       List<SendInntektsmeldingRequestDto.Refusjon> refusjonsendringRequestDto) {
        // Opphør og start ligger på egne felter, så disse skal ikke mappes som endringer.
        // Merk at opphørsdato er dagen før endring som opphører refusjon, derfor må vi legge til en dag.
        return refusjonsendringRequestDto.stream()
            .filter(r -> !r.fom().equals(startdato))
            .filter(r -> !r.fom().equals(opphørsdato.plusDays(1)))
            .map(dto -> new RefusjonsendringEntitet(dto.fom(), dto.beløp()))
            .toList();
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(List<SendInntektsmeldingRequestDto.BortfaltNaturalytelseRequestDto> dto) {
        return dto.stream()
            .map(d -> new BortaltNaturalytelseEntitet.Builder().medPeriode(d.fom(), d.tom() != null ? d.tom() : Tid.TIDENES_ENDE)
                .medMånedBeløp(d.beløp())
                .medType(KodeverkMapper.mapNaturalytelseTilEntitet(d.naturalytelsetype()))
                .build())
            .toList();
    }

    private static KontaktpersonEntitet mapKontaktPerson(SendInntektsmeldingRequestDto dto) {
        return new KontaktpersonEntitet(dto.kontaktperson().navn(), dto.kontaktperson().telefonnummer());
    }

    private static OmsorgspengerEntitet mapOmsorgspengerFraForespørsel(List<PeriodeDto> etterspurtePerioder) {
        return OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true) // Forespørsel fra omsorgspenger har alltid betalt pliktige dager
            .medFraværsPerioder(mapPerioder(etterspurtePerioder))
            .build();
    }


    private static OmsorgspengerEntitet mapOmsorgspenger(OmsorgspengerRequestDto dto) {
        return OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(dto.harUtbetaltPliktigeDager())
            .medFraværsPerioder(mapFraværsPerioder(dto.fraværHeleDager()))
            .medDelvisFraværsPerioder(mapDelvisFraværsPerioder(dto.fraværDelerAvDagen()))
            .build();
    }

    private static List<FraværsPeriodeEntitet> mapPerioder(List<PeriodeDto> etterspurtePerioder) {
        return etterspurtePerioder.stream()
            .map(fraværsPeriode -> new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(fraværsPeriode.fom(), fraværsPeriode.tom())))
            .toList();
    }

    private static List<FraværsPeriodeEntitet> mapFraværsPerioder(List<OmsorgspengerRequestDto.FraværHeleDagerRequestDto> dto) {
        if (dto == null) {
            return null;
        }
        return dto.stream()
            .map(fraværsPeriode -> new FraværsPeriodeEntitet(PeriodeEntitet.fraOgMedTilOgMed(fraværsPeriode.fom(), fraværsPeriode.tom())))
            .toList();
    }

    private static List<DelvisFraværsPeriodeEntitet> mapDelvisFraværsPerioder(List<OmsorgspengerRequestDto.FraværDelerAvDagenRequestDto> dto) {
        if (dto == null) {
            return null;
        }
        return dto.stream()
            .map(delvisFraværsPeriode -> new DelvisFraværsPeriodeEntitet(delvisFraværsPeriode.dato(), delvisFraværsPeriode.timer()))
            .toList();
    }
}
