package no.nav.familie.inntektsmelding.imapi.inntektsmelding;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.EndringsårsakEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.LpsSystemInfoEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonsendringEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.ForespørselType;
import no.nav.familie.inntektsmelding.koder.InntektsmeldingType;
import no.nav.familie.inntektsmelding.koder.Kildesystem;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.k9.inntektsmelding.felles.AvsenderSystemDto;
import no.nav.k9.inntektsmelding.felles.BortfaltNaturalytelseDto;
import no.nav.k9.inntektsmelding.felles.EndringsårsakerDto;
import no.nav.k9.inntektsmelding.felles.FødselsnummerDto;
import no.nav.k9.inntektsmelding.felles.KontaktpersonDto;
import no.nav.k9.inntektsmelding.felles.NaturalytelsetypeDto;
import no.nav.k9.inntektsmelding.felles.OrganisasjonsnummerDto;
import no.nav.k9.inntektsmelding.felles.RefusjonDto;
import no.nav.k9.inntektsmelding.felles.YtelseTypeDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.InntektsmeldingDto;
import no.nav.k9.inntektsmelding.imapi.inntektsmelding.SendInntektsmeldingRequest;
import no.nav.vedtak.konfig.Tid;

class InntektsmeldingApiMapper {

    private InntektsmeldingApiMapper() {}

    static InntektsmeldingEntitet mapTilEntitet(SendInntektsmeldingRequest request,
                                                AktørIdEntitet aktørId,
                                                ForespørselEntitet forespørsel) {
        var refusjonPrMnd = finnFørsteRefusjon(request.refusjon(), request.startdato()).orElse(null);
        var opphørsdato = refusjonPrMnd == null ? null : finnOpphørsdato(request.refusjon(), request.startdato()).orElse(Tid.TIDENES_ENDE);

        var lpsSystem = LpsSystemInfoEntitet.builder()
            .medNavn(request.avsenderSystem().systemNavn())
            .medVersjon(request.avsenderSystem().systemVersjon())
            .build();

        return InntektsmeldingEntitet.builder()
            .medAktørId(aktørId)
            .medArbeidsgiverIdent(request.organisasjonsnummer().orgnr())
            .medMånedInntekt(request.inntekt())
            .medKildesystem(Kildesystem.LØNN_OG_PERSONAL_SYSTEM)
            .medInntektsmeldingType(utledInntektsmeldingType(forespørsel.getForespørselType()))
            .medMånedRefusjon(refusjonPrMnd)
            .medRefusjonOpphørsdato(opphørsdato)
            .medStartDato(request.startdato())
            .medYtelsetype(mapYtelsetype(request.ytelseType()))
            .medKontaktperson(new KontaktpersonEntitet(request.kontaktperson().navn(), request.kontaktperson().telefonnummer()))
            .medEndringsårsaker(mapEndringsårsaker(request.endringAvInntektÅrsaker()))
            .medBortfaltNaturalytelser(mapBortfalteNaturalytelser(request.bortfaltNaturalytelsePerioder()))
            .medRefusjonsendringer(mapRefusjonsendringer(request.startdato(), opphørsdato, request.refusjon()))
            .medLpsSystemInfo(lpsSystem)
            .medForespørsel(forespørsel)
            .build();
    }

    static InntektsmeldingDto mapFraEntitet(InntektsmeldingEntitet inntektsmelding, PersonIdent personIdent) {
        var forespørsel = inntektsmelding.getForespørsel();
        if (personIdent == null) {
            throw new IllegalArgumentException("Finner ikke fødselsnummer for aktørId når personIdent er null." );
        }
            return new InntektsmeldingDto(
            inntektsmelding.getUuid(),
            forespørsel.getUuid(),
            new FødselsnummerDto(personIdent.getIdent()),
            mapYtelseTypeTilKontrakt(inntektsmelding.getYtelsetype()),
            new OrganisasjonsnummerDto(inntektsmelding.getArbeidsgiverIdent()),
            mapKontaktperson(inntektsmelding),
            inntektsmelding.getStartDato(),
            inntektsmelding.getMånedInntekt(),
            inntektsmelding.getOpprettetTidspunkt(),
            inntektsmelding.getMånedRefusjon(),
            inntektsmelding.getOpphørsdatoRefusjon(),
            utledAvsenderSystem(inntektsmelding),
            mapRefusjonsendringerTilKontrakt(inntektsmelding),
            mapBortfalteNaturalytelserTilKontrakt(inntektsmelding.getBorfalteNaturalYtelser()),
            mapEndringsårsakerTilKontrakt(inntektsmelding.getEndringsårsaker())
        );
    }

    private static KontaktpersonDto mapKontaktperson(InntektsmeldingEntitet inntektsmelding) {
        return new KontaktpersonDto(inntektsmelding.getKontaktperson().getNavn(), inntektsmelding.getKontaktperson().getTelefonnummer());
    }

    private static InntektsmeldingType utledInntektsmeldingType(ForespørselType forespørselType) {
        return switch (forespørselType) {
            case ARBEIDSGIVERINITIERT_NYANSATT -> InntektsmeldingType.ARBEIDSGIVERINITIERT_NYANSATT;
            case ARBEIDSGIVERINITIERT_UREGISTRERT -> InntektsmeldingType.ARBEIDSGIVERINITIERT_UREGISTRERT;
            case OMSORGSPENGER_REFUSJON -> InntektsmeldingType.OMSORGSPENGER_REFUSJON;
            case BESTILT_AV_FAGSYSTEM, BESTILT_AV_SAKSBEHANDLER -> InntektsmeldingType.ORDINÆR;
        };
    }

    private static AvsenderSystemDto utledAvsenderSystem(InntektsmeldingEntitet entitet) {
        if (entitet.getLpsSystem() != null) {
            return new AvsenderSystemDto(entitet.getLpsSystem().getNavn(), entitet.getLpsSystem().getVersjon());
        }
        return new AvsenderSystemDto("NAV_NO", "1.0");
    }

    static Ytelsetype mapYtelsetype(YtelseTypeDto ytelseTypeDto) {
        return switch (ytelseTypeDto) {
            case OMSORGSPENGER -> Ytelsetype.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> Ytelsetype.OPPLÆRINGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> Ytelsetype.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_I_LIVETS_SLUTTFASE -> Ytelsetype.PLEIEPENGER_NÆRSTÅENDE;
        };
    }

    private static YtelseTypeDto mapYtelseTypeTilKontrakt(Ytelsetype ytelsetype) {
        return switch (ytelsetype) {
            case OMSORGSPENGER -> YtelseTypeDto.OMSORGSPENGER;
            case OPPLÆRINGSPENGER -> YtelseTypeDto.OPPLÆRINGSPENGER;
            case PLEIEPENGER_SYKT_BARN -> YtelseTypeDto.PLEIEPENGER_SYKT_BARN;
            case PLEIEPENGER_NÆRSTÅENDE -> YtelseTypeDto.PLEIEPENGER_I_LIVETS_SLUTTFASE;
        };
    }

    private static Optional<BigDecimal> finnFørsteRefusjon(List<RefusjonDto> refusjon, LocalDate startdato) {
        if (refusjon.isEmpty()) {
            return Optional.empty();
        }
        var refusjonPåStartdato = refusjon.stream().filter(r -> r.fom().equals(startdato)).toList();
        if (refusjonPåStartdato.size() != 1) {
            throw new IllegalStateException("Forventer kun 1 refusjon som starter på startdato, fant " + refusjonPåStartdato.size());
        }
        return Optional.of(refusjonPåStartdato.getFirst().beløp());
    }

    private static Optional<LocalDate> finnOpphørsdato(List<RefusjonDto> refusjon, LocalDate startdato) {
        // Hvis siste endring setter refusjon til 0 er det å regne som opphør av refusjon,
        // setter dagen før denne endringen som opphørsdato
        return refusjon.stream()
            .filter(r -> !r.fom().equals(startdato))
            .max(Comparator.comparing(RefusjonDto::fom))
            .filter(r -> r.beløp().compareTo(BigDecimal.ZERO) == 0)
            .map(r -> r.fom().minusDays(1));
    }

    private static List<RefusjonsendringEntitet> mapRefusjonsendringer(LocalDate startdato,
                                                                        LocalDate opphørsdato,
                                                                        List<RefusjonDto> refusjon) {
        // Opphør og start ligger på egne felter, så disse skal ikke mappes som endringer.
        // Merk at opphørsdato er dagen før endring som opphører refusjon, derfor må vi legge til en dag.
        return refusjon.stream()
            .filter(r -> !r.fom().equals(startdato))
            .filter(r -> opphørsdato == null || !r.fom().equals(opphørsdato.plusDays(1)))
            .map(r -> new RefusjonsendringEntitet(r.fom(), r.beløp()))
            .toList();
    }

    private static List<BortaltNaturalytelseEntitet> mapBortfalteNaturalytelser(List<BortfaltNaturalytelseDto> dto) {
        return dto.stream()
            .map(d -> BortaltNaturalytelseEntitet.builder()
                .medPeriode(d.fom(), d.tom() != null ? d.tom() : Tid.TIDENES_ENDE)
                .medMånedBeløp(d.beløp())
                .medType(NaturalytelseType.valueOf(d.naturalytelsetype().name()))
                .build())
            .toList();
    }

    private static List<EndringsårsakEntitet> mapEndringsårsaker(List<EndringsårsakerDto> endringsårsaker) {
        return endringsårsaker.stream()
            .map(endringsårsak -> EndringsårsakEntitet.builder()
                .medÅrsak(Endringsårsak.valueOf(endringsårsak.årsak().name()))
                .medFom(endringsårsak.fom())
                .medTom(endringsårsak.tom())
                .medBleKjentFra(endringsårsak.bleKjentFom())
                .build())
            .toList();
    }

    private static List<RefusjonDto> mapRefusjonsendringerTilKontrakt(InntektsmeldingEntitet entitet) {
        List<RefusjonDto> refusjoner = new ArrayList<>();
        if (entitet.getMånedRefusjon() != null) {
            refusjoner.add(new RefusjonDto(entitet.getStartDato(), entitet.getMånedRefusjon()));
        }
        if (entitet.getOpphørsdatoRefusjon() != null && !Tid.TIDENES_ENDE.equals(entitet.getOpphørsdatoRefusjon())) {
            refusjoner.add(new RefusjonDto(entitet.getOpphørsdatoRefusjon().plusDays(1), BigDecimal.ZERO));
        }
        entitet.getRefusjonsendringer().stream()
            .map(r -> new RefusjonDto(r.getFom(), r.getRefusjonPrMnd()))
            .forEach(refusjoner::add);
        return refusjoner.stream().sorted(Comparator.comparing(RefusjonDto::fom)).toList();
    }

    private static List<BortfaltNaturalytelseDto> mapBortfalteNaturalytelserTilKontrakt(List<BortaltNaturalytelseEntitet> bortfalte) {
        return bortfalte.stream()
            .map(b -> new BortfaltNaturalytelseDto(
                b.getPeriode().getFom(),
                Tid.TIDENES_ENDE.equals(b.getPeriode().getTom()) ? null : b.getPeriode().getTom(),
                NaturalytelsetypeDto.valueOf(b.getType().name()),
                b.getMånedBeløp()))
            .toList();
    }

    private static List<EndringsårsakerDto> mapEndringsårsakerTilKontrakt(List<EndringsårsakEntitet> endringsårsaker) {
        return endringsårsaker.stream()
            .map(e -> new EndringsårsakerDto(
                no.nav.k9.inntektsmelding.felles.EndringsårsakDto.valueOf(e.getÅrsak().name()),
                e.getFom().orElse(null),
                e.getTom().orElse(null),
                e.getBleKjentFom().orElse(null)))
            .toList();
    }
}
