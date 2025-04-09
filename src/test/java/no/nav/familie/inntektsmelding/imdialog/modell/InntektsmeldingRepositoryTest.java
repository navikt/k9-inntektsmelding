package no.nav.familie.inntektsmelding.imdialog.modell;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.familie.inntektsmelding.database.JpaExtension;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselRepository;
import no.nav.familie.inntektsmelding.koder.Endringsårsak;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.testutilities.db.EntityManagerAwareTest;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(JpaExtension.class)
class InntektsmeldingRepositoryTest extends EntityManagerAwareTest {

    private InntektsmeldingRepository inntektsmeldingRepository;
    private ForespørselRepository forespørselRepository;

    private static final AktørIdEntitet AKTØR_ID = new AktørIdEntitet("9999999999999");
    private static final String ARBEIDSGIVER_IDENT = "999999999";
    private static final LocalDate START_DATO = LocalDate.now();
    private static final String SAKSNUMMER = "saksnummer1";
    private static final Ytelsetype YTELSETYPE = Ytelsetype.PLEIEPENGER_SYKT_BARN;

    @BeforeEach
    void setUp() {
        this.inntektsmeldingRepository = new InntektsmeldingRepository(getEntityManager());
        this.forespørselRepository = new ForespørselRepository(getEntityManager());
    }


    @Test
    void skal_lagre_inntektsmelding() {
        // Arrange
        var forespørselId = forespørselRepository.lagreForespørsel(START_DATO, YTELSETYPE, AKTØR_ID.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO);
        var forespørsel = forespørselRepository.hentForespørsel(forespørselId);

        var imFørLagring = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(YTELSETYPE)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medStartDato(START_DATO)
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medForespørsel(forespørsel.get())
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(imFørLagring);

        clearHibernateCache();

        var imEtterLagring = inntektsmeldingRepository.hentInntektsmelding(imFørLagring.getId());

        // Assert
        assertThat(imEtterLagring.getKontaktperson().getTelefonnummer()).isEqualTo(imFørLagring.getKontaktperson().getTelefonnummer());
        assertThat(imEtterLagring.getKontaktperson().getNavn()).isEqualTo(imFørLagring.getKontaktperson().getNavn());
        assertThat(imEtterLagring.getMånedInntekt()).isEqualByComparingTo(imFørLagring.getMånedInntekt());
        assertThat(imEtterLagring.getArbeidsgiverIdent()).isEqualTo(imFørLagring.getArbeidsgiverIdent());
        assertThat(imEtterLagring.getAktørId()).isEqualTo(imFørLagring.getAktørId());
        assertThat(imEtterLagring.getStartDato()).isEqualTo(imFørLagring.getStartDato());
        assertThat(imEtterLagring.getYtelsetype()).isEqualTo(imFørLagring.getYtelsetype());
    }


    @Test
    void skal_lagre_inntektsmelding_med_refusjon() {
        // Arrange
        var forespørselUuid = forespørselRepository.lagreForespørsel(START_DATO, YTELSETYPE, AKTØR_ID.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO);
        var forespørsel = forespørselRepository.hentForespørsel(forespørselUuid);

        var imFørLagring = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(YTELSETYPE)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO.plusDays(3), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medForespørsel(forespørsel.get())
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(imFørLagring);

        clearHibernateCache();

        var imEtterLagring = inntektsmeldingRepository.hentInntektsmelding(imFørLagring.getId());

        // Assert
        assertThat(imEtterLagring.getKontaktperson().getTelefonnummer()).isEqualTo(imFørLagring.getKontaktperson().getTelefonnummer());
        assertThat(imEtterLagring.getKontaktperson().getNavn()).isEqualTo(imFørLagring.getKontaktperson().getNavn());
        assertThat(imEtterLagring.getMånedInntekt()).isEqualByComparingTo(imFørLagring.getMånedInntekt());
        assertThat(imEtterLagring.getArbeidsgiverIdent()).isEqualTo(imFørLagring.getArbeidsgiverIdent());
        assertThat(imEtterLagring.getAktørId()).isEqualTo(imFørLagring.getAktørId());
        assertThat(imEtterLagring.getStartDato()).isEqualTo(imFørLagring.getStartDato());
        assertThat(imEtterLagring.getYtelsetype()).isEqualTo(imFørLagring.getYtelsetype());
        assertThat(imEtterLagring.getOpphørsdatoRefusjon()).isEqualTo(imFørLagring.getOpphørsdatoRefusjon());
        assertThat(imEtterLagring.getMånedRefusjon()).isEqualByComparingTo(imFørLagring.getMånedRefusjon());
        assertThat(imEtterLagring.getRefusjonsendringer()).hasSize(1);
        assertThat(imEtterLagring.getRefusjonsendringer().getFirst().getFom()).isEqualTo(imFørLagring.getRefusjonsendringer().getFirst().getFom());
        assertThat(imEtterLagring.getRefusjonsendringer().getFirst().getRefusjonPrMnd()).isEqualByComparingTo(imFørLagring.getRefusjonsendringer()
            .getFirst()
            .getRefusjonPrMnd());
    }


    @Test
    void skal_lagre_inntektsmelding_med_endringsårsaker() {
        // Arrange
        var forespørselId = forespørselRepository.lagreForespørsel(START_DATO, YTELSETYPE, AKTØR_ID.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO);
        var forespørsel = forespørselRepository.hentForespørsel(forespørselId);

        var endring = EndringsårsakEntitet.builder()
            .medÅrsak(Endringsårsak.TARIFFENDRING)
            .medFom(LocalDate.now())
            .medBleKjentFra(LocalDate.now().plusDays(10))
            .build();

        var imFørLagring = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(YTELSETYPE)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO)
            .medEndringsårsaker(Collections.singletonList(endring))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medForespørsel(forespørsel.get())
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(imFørLagring);

        clearHibernateCache();

        var imEtterLagring = inntektsmeldingRepository.hentInntektsmelding(imFørLagring.getId());

        // Assert
        assertThat(imEtterLagring.getKontaktperson().getTelefonnummer()).isEqualTo(imFørLagring.getKontaktperson().getTelefonnummer());
        assertThat(imEtterLagring.getKontaktperson().getNavn()).isEqualTo(imFørLagring.getKontaktperson().getNavn());
        assertThat(imEtterLagring.getMånedInntekt()).isEqualByComparingTo(imFørLagring.getMånedInntekt());
        assertThat(imEtterLagring.getArbeidsgiverIdent()).isEqualTo(imFørLagring.getArbeidsgiverIdent());
        assertThat(imEtterLagring.getAktørId()).isEqualTo(imFørLagring.getAktørId());
        assertThat(imEtterLagring.getStartDato()).isEqualTo(imFørLagring.getStartDato());
        assertThat(imEtterLagring.getYtelsetype()).isEqualTo(imFørLagring.getYtelsetype());
        assertThat(imEtterLagring.getOpphørsdatoRefusjon()).isEqualTo(imFørLagring.getOpphørsdatoRefusjon());
        assertThat(imEtterLagring.getMånedRefusjon()).isEqualByComparingTo(imFørLagring.getMånedRefusjon());
        assertThat(imEtterLagring.getEndringsårsaker()).hasSize(1);
        assertThat(imEtterLagring.getEndringsårsaker().getFirst().getÅrsak()).isEqualTo(Endringsårsak.TARIFFENDRING);
        assertThat(imEtterLagring.getEndringsårsaker().getFirst().getFom().orElse(null)).isEqualTo(LocalDate.now());
        assertThat(imEtterLagring.getEndringsårsaker().getFirst().getBleKjentFom().orElse(null)).isEqualTo(LocalDate.now().plusDays(10));
        assertThat(imEtterLagring.getOmsorgspenger()).isNull();
    }


    @Test
    void skal_lagre_inntektsmelding_med_omsorgspenger() {
        // Arrange
        var forespørselId = forespørselRepository.lagreForespørsel(START_DATO, YTELSETYPE, AKTØR_ID.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO);
        var forespørsel = forespørselRepository.hentForespørsel(forespørselId);

        var forventetFraværsPeriode1 = PeriodeEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(10));
        var forventetFraværsPeriode2 = PeriodeEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(2), LocalDate.now().minusMonths(1));
        var forventetDelvisFraværsDato = LocalDate.now().plusDays(11);
        var forventetDelvisFraværsTimer = BigDecimal.valueOf(4);

        var omsorgspenger = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true)
            .medFraværsPerioder(List.of(new FraværsPeriodeEntitet(forventetFraværsPeriode1), new FraværsPeriodeEntitet(forventetFraværsPeriode2)))
            .medDelvisFraværsPerioder(List.of(new DelvisFraværsPeriodeEntitet(forventetDelvisFraværsDato, forventetDelvisFraværsTimer)))
            .build();

        var imFørLagring = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO)
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOmsorgspenger(omsorgspenger)
            .medForespørsel(forespørsel.get())
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(imFørLagring);

        clearHibernateCache();

        var imEtterLagring = inntektsmeldingRepository.hentInntektsmelding(imFørLagring.getId());

        // Assert
        assertThat(imEtterLagring.getKontaktperson().getTelefonnummer()).isEqualTo(imFørLagring.getKontaktperson().getTelefonnummer());
        assertThat(imEtterLagring.getKontaktperson().getNavn()).isEqualTo(imFørLagring.getKontaktperson().getNavn());
        assertThat(imEtterLagring.getMånedInntekt()).isEqualByComparingTo(imFørLagring.getMånedInntekt());
        assertThat(imEtterLagring.getArbeidsgiverIdent()).isEqualTo(imFørLagring.getArbeidsgiverIdent());
        assertThat(imEtterLagring.getAktørId()).isEqualTo(imFørLagring.getAktørId());
        assertThat(imEtterLagring.getStartDato()).isEqualTo(imFørLagring.getStartDato());
        assertThat(imEtterLagring.getYtelsetype()).isEqualTo(imFørLagring.getYtelsetype());
        assertThat(imEtterLagring.getOpphørsdatoRefusjon()).isEqualTo(imFørLagring.getOpphørsdatoRefusjon());
        assertThat(imEtterLagring.getMånedRefusjon()).isEqualByComparingTo(imFørLagring.getMånedRefusjon());
        assertThat(imEtterLagring.getEndringsårsaker()).hasSize(0);

        assertThat(imEtterLagring.getOmsorgspenger().isHarUtbetaltPliktigeDager()).isTrue();
        assertThat(imEtterLagring.getOmsorgspenger().getFraværsPerioder()).hasSize(2);
        assertThat(imEtterLagring.getOmsorgspenger().getFraværsPerioder().getFirst().getPeriode().getFom()).isEqualTo(forventetFraværsPeriode1.getFom());
        assertThat(imEtterLagring.getOmsorgspenger().getFraværsPerioder().getFirst().getPeriode().getTom()).isEqualTo(forventetFraværsPeriode1.getTom());
        assertThat(imEtterLagring.getOmsorgspenger().getFraværsPerioder().getLast().getPeriode().getFom()).isEqualTo(forventetFraværsPeriode2.getFom());
        assertThat(imEtterLagring.getOmsorgspenger().getFraværsPerioder().getLast().getPeriode().getTom()).isEqualTo(forventetFraværsPeriode2.getTom());
        assertThat(imEtterLagring.getOmsorgspenger().getDelvisFraværsPerioder()).hasSize(1);
        assertThat(imEtterLagring.getOmsorgspenger().getDelvisFraværsPerioder().getFirst().getDato()).isEqualTo(forventetDelvisFraværsDato);
    }


    @Test
    void skal_lagre_inntektsmelding_med_omsorgspenger_med_kun_delvis_fravær() {
        // Arrange
        var forespørselId = forespørselRepository.lagreForespørsel(START_DATO, YTELSETYPE, AKTØR_ID.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO);
        var forespørsel = forespørselRepository.hentForespørsel(forespørselId);

        var forventetDelvisFraværsDato = LocalDate.now().plusDays(11);
        var forventetDelvisFraværsTimer = BigDecimal.valueOf(4);

        var omsorgspenger = OmsorgspengerEntitet.builder()
            .medHarUtbetaltPliktigeDager(true)
            .medFraværsPerioder(null)
            .medDelvisFraværsPerioder(List.of(new DelvisFraværsPeriodeEntitet(forventetDelvisFraværsDato, forventetDelvisFraværsTimer)))
            .build();

        var imFørLagring = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medKontaktperson(new KontaktpersonEntitet("Testy test", "999999999"))
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO)
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOmsorgspenger(omsorgspenger)
            .medForespørsel(forespørsel.get())
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(imFørLagring);

        clearHibernateCache();

        var imEtterLagring = inntektsmeldingRepository.hentInntektsmelding(imFørLagring.getId());

        // Assert
        assertThat(imEtterLagring.getKontaktperson().getTelefonnummer()).isEqualTo(imFørLagring.getKontaktperson().getTelefonnummer());
        assertThat(imEtterLagring.getKontaktperson().getNavn()).isEqualTo(imFørLagring.getKontaktperson().getNavn());
        assertThat(imEtterLagring.getMånedInntekt()).isEqualByComparingTo(imFørLagring.getMånedInntekt());
        assertThat(imEtterLagring.getArbeidsgiverIdent()).isEqualTo(imFørLagring.getArbeidsgiverIdent());
        assertThat(imEtterLagring.getAktørId()).isEqualTo(imFørLagring.getAktørId());
        assertThat(imEtterLagring.getStartDato()).isEqualTo(imFørLagring.getStartDato());
        assertThat(imEtterLagring.getYtelsetype()).isEqualTo(imFørLagring.getYtelsetype());
        assertThat(imEtterLagring.getOpphørsdatoRefusjon()).isEqualTo(imFørLagring.getOpphørsdatoRefusjon());
        assertThat(imEtterLagring.getMånedRefusjon()).isEqualByComparingTo(imFørLagring.getMånedRefusjon());
        assertThat(imEtterLagring.getEndringsårsaker()).hasSize(0);

        assertThat(imEtterLagring.getOmsorgspenger().isHarUtbetaltPliktigeDager()).isTrue();
        assertThat(imEtterLagring.getOmsorgspenger().getFraværsPerioder()).hasSize(0);
        assertThat(imEtterLagring.getOmsorgspenger().getDelvisFraværsPerioder()).hasSize(1);
        assertThat(imEtterLagring.getOmsorgspenger().getDelvisFraværsPerioder().getFirst().getDato()).isEqualTo(forventetDelvisFraværsDato);
    }


    @Test
    void skal_hente_alle_im_for_forespørsel() {
        // Arrange
        var person1 = AKTØR_ID;
        var forespørselIdPsbPerson1 = forespørselRepository.lagreForespørsel(START_DATO, Ytelsetype.PLEIEPENGER_SYKT_BARN, person1.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO);
        var forespørselPsbPerson1 = forespørselRepository.hentForespørsel(forespørselIdPsbPerson1);

        AktørIdEntitet person2 = new AktørIdEntitet("1234567891111");
        var forespørselIdPsbPerson2 = forespørselRepository.lagreForespørsel(START_DATO, Ytelsetype.PLEIEPENGER_SYKT_BARN, person2.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO);
        var forespørselPsbPerson2 = forespørselRepository.hentForespørsel(forespørselIdPsbPerson2);

        var forespørselIdPpnPerson1 = forespørselRepository.lagreForespørsel(START_DATO, Ytelsetype.PLEIEPENGER_NÆRSTÅENDE, person1.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO);
        var forespørselPpnPerson1 = forespørselRepository.hentForespørsel(forespørselIdPpnPerson1);

        var im1 = InntektsmeldingEntitet.builder()
            .medAktørId(person1)
            .medKontaktperson(new KontaktpersonEntitet("Første", "999999999"))
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(1))
            .medForespørsel(forespørselPsbPerson1.get())
            .build();

        var im2 = InntektsmeldingEntitet.builder()
            .medAktørId(person2)
            .medKontaktperson(new KontaktpersonEntitet("Andre", "999999999"))
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(2))
            .medForespørsel(forespørselPsbPerson2.get())
            .build();

        var im3 = InntektsmeldingEntitet.builder()
            .medAktørId(person1)
            .medKontaktperson(new KontaktpersonEntitet("Tredje", "999999999"))
            .medYtelsetype(Ytelsetype.PLEIEPENGER_SYKT_BARN)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(3))
            .medForespørsel(forespørselPsbPerson1.get())
            .build();

        var im4 = InntektsmeldingEntitet.builder()
            .medAktørId(person1)
            .medKontaktperson(new KontaktpersonEntitet("Fjerde", "999999999"))
            .medYtelsetype(Ytelsetype.PLEIEPENGER_NÆRSTÅENDE)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(4))
            .medForespørsel(forespørselPpnPerson1.get())
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(im1);
        inntektsmeldingRepository.lagreInntektsmelding(im2);
        inntektsmeldingRepository.lagreInntektsmelding(im3);
        inntektsmeldingRepository.lagreInntektsmelding(im4);

        clearHibernateCache();

        var forespørselPsbPerson1EtterLagring = forespørselRepository.hentForespørsel(forespørselIdPsbPerson1).get();
        var imEtterLagringPsbPerson1 = forespørselPsbPerson1EtterLagring.getInntektsmeldinger();

        var forespørselPsbPerson2EtterLagring = forespørselRepository.hentForespørsel(forespørselIdPsbPerson2).get();
        var imEtterLagringPsbPerson2 = forespørselPsbPerson2EtterLagring.getInntektsmeldinger();

        var forespørselPpnPerson1EtterLagring = forespørselRepository.hentForespørsel(forespørselIdPpnPerson1).get();
        var imEtterLagringPpnPerson1 = forespørselPpnPerson1EtterLagring.getInntektsmeldinger();

        // Assert
        assertThat(imEtterLagringPsbPerson1).hasSize(2);
        assertThat(imEtterLagringPsbPerson1.get(0).getKontaktperson().getNavn()).isEqualTo(im3.getKontaktperson().getNavn());
        assertThat(imEtterLagringPsbPerson1.get(0).getYtelsetype()).isEqualTo(im3.getYtelsetype());
        assertThat(imEtterLagringPsbPerson1.get(1).getKontaktperson().getNavn()).isEqualTo(im1.getKontaktperson().getNavn());
        assertThat(imEtterLagringPsbPerson1.get(1).getYtelsetype()).isEqualTo(im1.getYtelsetype());

        assertThat(imEtterLagringPsbPerson2).hasSize(1);
        assertThat(imEtterLagringPsbPerson2.get(0).getKontaktperson().getNavn()).isEqualTo(im2.getKontaktperson().getNavn());
        assertThat(imEtterLagringPsbPerson2.get(0).getYtelsetype()).isEqualTo(im2.getYtelsetype());

        assertThat(imEtterLagringPpnPerson1).hasSize(1);
        assertThat(imEtterLagringPpnPerson1.get(0).getKontaktperson().getNavn()).isEqualTo(im4.getKontaktperson().getNavn());
        assertThat(imEtterLagringPpnPerson1.get(0).getYtelsetype()).isEqualTo(im4.getYtelsetype());
    }

    @Test
    void skal_hente_alle_im_for_ett_gitt_år() {
        // Arrange
        var START_DATO_2025 = LocalDate.of(2025, 2, 1);
        var START_DATO_2024 = LocalDate.of(2024, 2, 1);

        var forespørselId = forespørselRepository.lagreForespørsel(START_DATO_2025, YTELSETYPE, AKTØR_ID.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO_2025);
        var forespørsel = forespørselRepository.hentForespørsel(forespørselId);

        AktørIdEntitet AKTØR_ID_2 = new AktørIdEntitet("1234567891111");
        var forespørselId2 = forespørselRepository.lagreForespørsel(START_DATO_2025, YTELSETYPE, AKTØR_ID_2.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO_2025);
        var forespørsel2 = forespørselRepository.hentForespørsel(forespørselId2);

        var forespørselId3 = forespørselRepository.lagreForespørsel(START_DATO_2024, YTELSETYPE, AKTØR_ID.getAktørId(), ARBEIDSGIVER_IDENT, SAKSNUMMER, START_DATO_2024);
        var forespørsel3 = forespørselRepository.hentForespørsel(forespørselId3);

        var im1 = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medKontaktperson(new KontaktpersonEntitet("Første", "999999999"))
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO_2025)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO_2025.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(1))
            .medForespørsel(forespørsel.get())
            .build();

        var im2 = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medKontaktperson(new KontaktpersonEntitet("Andre", "999999999"))
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO_2025)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO_2025.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(2))
            .medForespørsel(forespørsel.get())
            .build();

        var im3RiktigÅrMenAnnenAktørId = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID_2)
            .medKontaktperson(new KontaktpersonEntitet("Tredje", "999999999"))
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO_2025)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO_2025.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOpprettetTidspunkt(LocalDateTime.now().plusDays(3))
            .medForespørsel(forespørsel2.get())
            .build();

        var im4i2024 = InntektsmeldingEntitet.builder()
            .medAktørId(AKTØR_ID)
            .medKontaktperson(new KontaktpersonEntitet("Fjerde", "999999999"))
            .medYtelsetype(Ytelsetype.OMSORGSPENGER)
            .medMånedInntekt(BigDecimal.valueOf(4000))
            .medMånedRefusjon(BigDecimal.valueOf(4000))
            .medRefusjonOpphørsdato(Tid.TIDENES_ENDE)
            .medStartDato(START_DATO_2024)
            .medRefusjonsendringer(Collections.singletonList(new RefusjonsendringEntitet(START_DATO_2024.plusDays(10), BigDecimal.valueOf(2000))))
            .medArbeidsgiverIdent(ARBEIDSGIVER_IDENT)
            .medOpprettetTidspunkt(LocalDateTime.now().minusYears(1).plusDays(4))
            .medForespørsel(forespørsel3.get())
            .build();

        // Act
        inntektsmeldingRepository.lagreInntektsmelding(im1);
        inntektsmeldingRepository.lagreInntektsmelding(im2);
        inntektsmeldingRepository.lagreInntektsmelding(im3RiktigÅrMenAnnenAktørId);
        inntektsmeldingRepository.lagreInntektsmelding(im4i2024);

        clearHibernateCache();

        var etterLagring = inntektsmeldingRepository.hentInntektsmeldingerForÅr(AKTØR_ID, ARBEIDSGIVER_IDENT, 2025, Ytelsetype.OMSORGSPENGER);

        // Assert
        assertThat(etterLagring).hasSize(2);
        assertThat(etterLagring.getFirst().getStartDato().getYear()).isEqualTo(2025);
        assertThat(etterLagring.getFirst().getAktørId()).isEqualTo(AKTØR_ID);
        assertThat(etterLagring.getFirst().getArbeidsgiverIdent()).isEqualTo(ARBEIDSGIVER_IDENT);
        assertThat(etterLagring.getLast().getStartDato().getYear()).isEqualTo(2025);
        assertThat(etterLagring.getLast().getAktørId()).isEqualTo(AKTØR_ID);
        assertThat(etterLagring.getLast().getArbeidsgiverIdent()).isEqualTo(ARBEIDSGIVER_IDENT);
    }


    private void clearHibernateCache() {
        // Fjerne hibernate cachen før assertions skal evalueres - hibernate ignorerer alle updates som er markert med updatable = false ved skriving mot databasen
        // men objektene i cachen blir oppdatert helt greit likevel.
        // På denne måten evaluerer vi faktisk tilstanden som blir til slutt lagret i databasen.
        getEntityManager().clear();
    }
}
