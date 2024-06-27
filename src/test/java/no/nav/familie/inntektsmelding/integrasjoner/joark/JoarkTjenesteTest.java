package no.nav.familie.inntektsmelding.integrasjoner.joark;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.KontaktpersonEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.NaturalytelseEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.RefusjonPeriodeEntitet;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.Organisasjon;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Naturalytelsetype;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.integrasjon.dokarkiv.dto.OpprettJournalpostResponse;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class JoarkTjenesteTest {

    @Mock
    private PersonTjeneste personTjeneste;

    @Mock
    private OrganisasjonTjeneste organisasjonTjeneste;

    @Mock
    private JoarkKlient klient;
    private static final byte[] PDFSIGNATURE = { 0x25, 0x50, 0x44, 0x46, 0x2d};

    private JoarkTjeneste joarkTjeneste;

    @BeforeEach
    void setup() {
        joarkTjeneste = new JoarkTjeneste(klient, organisasjonTjeneste, personTjeneste);
    }

    @Test
    void skal_teste_oversending_organisasjon() {
        // Arrange
        var aktørIdSøker = new AktørIdEntitet("1234567891234");
        var refusjonperiode = new RefusjonPeriodeEntitet(LocalDate.of(2024, 6, 1), Tid.TIDENES_ENDE, BigDecimal.valueOf(35000));
        var naturalytelse = NaturalytelseEntitet.builder()
            .medPeriode(LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 30))
            .medType(Naturalytelsetype.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medErBortfalt(true)
            .medBeløp(BigDecimal.valueOf(2000))
            .build();
        var arbeidsgiverIdent = "999999999";
        var inntektsmelding = InntektsmeldingEntitet.builder()
            .medArbeidsgiverIdent(arbeidsgiverIdent)
            .medStartDato(LocalDate.of(2024, 6, 1))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medOpprettetTidspunkt(LocalDateTime.now())
            .medKontaktperson(new KontaktpersonEntitet("Test Testen", "111111111"))
            .medNaturalYtelse(Collections.singletonList(naturalytelse))
            .medRefusjonsPeriode(Collections.singletonList(refusjonperiode))
            .build();

        var testBedrift = new Organisasjon("Test Bedrift", arbeidsgiverIdent);

        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
        when(organisasjonTjeneste.finnOrganisasjon(arbeidsgiverIdent)).thenReturn(testBedrift);
        when(klient.opprettJournalpost(any(), anyBoolean())).thenReturn(new OpprettJournalpostResponse("9999", false, Collections.emptyList()));

        // Act
        var journalpostId = joarkTjeneste.journalførInntektsmelding("XML", inntektsmelding, PDFSIGNATURE);

        // Assert
        assertThat(journalpostId).isEqualTo("9999");
    }

    @Test
    void skal_teste_oversending_privapterson() {
        // Arrange
        var aktørIdSøker = new AktørIdEntitet("1234567891234");
        var refusjonperiode = new RefusjonPeriodeEntitet(LocalDate.of(2024, 6, 1), Tid.TIDENES_ENDE, BigDecimal.valueOf(35000));
        var naturalytelse = NaturalytelseEntitet.builder()
            .medPeriode(LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 30))
            .medType(Naturalytelsetype.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medErBortfalt(true)
            .medBeløp(BigDecimal.valueOf(2000))
            .build();
        var aktørIdArbeidsgiver = "2222222222222";
        var inntektsmelding = InntektsmeldingEntitet.builder()
            .medArbeidsgiverIdent(aktørIdArbeidsgiver)
            .medStartDato(LocalDate.of(2024, 6, 1))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medOpprettetTidspunkt(LocalDateTime.now())
            .medKontaktperson(new KontaktpersonEntitet("Test Testen", "111111111"))
            .medNaturalYtelse(Collections.singletonList(naturalytelse))
            .medRefusjonsPeriode(Collections.singletonList(refusjonperiode))
            .build();

        // Kan foreløpig ikke teste med spesifikk request i mock siden eksternreferanse genereres on the fly
         when(personTjeneste.hentPersonInfoFraAktørId(new AktørIdEntitet(aktørIdArbeidsgiver), Ytelsetype.FORELDREPENGER)).thenReturn(new PersonInfo("Navn",  null, "Navnesen", new PersonIdent("9999999999999"), aktørIdSøker, LocalDate.now()));
        when(klient.opprettJournalpost(any(), anyBoolean())).thenReturn(new OpprettJournalpostResponse("9999", false, Collections.emptyList()));
        // Act
        var journalpostId = joarkTjeneste.journalførInntektsmelding("XML", inntektsmelding, PDFSIGNATURE);

        // Assert
        assertThat(journalpostId).isEqualTo("9999");
    }
}
