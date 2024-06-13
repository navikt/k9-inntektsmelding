package no.nav.familie.inntektsmelding.imdialog.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Naturalytelsetype;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.konfig.Tid;

@ExtendWith(MockitoExtension.class)
class InntektsmeldingXMLTjenesteTest {

    @Mock
    private PersonTjeneste personTjeneste;

    private InntektsmeldingXMLTjeneste inntektsmeldingXMLTjeneste;

    @BeforeEach
    void setUp() {
        inntektsmeldingXMLTjeneste = new InntektsmeldingXMLTjeneste(personTjeneste);
    }

    @Test
    void skal_teste_xml_generering() {
        // Arrange
        var refusjonperiode = new RefusjonPeriodeEntitet(LocalDate.of(2024, 6, 1), Tid.TIDENES_ENDE, BigDecimal.valueOf(35000));
        var naturalytelse = NaturalytelseEntitet.builder()
            .medPeriode(LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 30))
            .medType(Naturalytelsetype.AKSJER_GRUNNFONDSBEVIS_TIL_UNDERKURS)
            .medErBortfalt(true)
            .medBeløp(BigDecimal.valueOf(2000))
            .build();
        var aktørIdSøker = new AktørIdEntitet("1234567891234");
        var fnrSøker = new PersonIdent("11111111111");
        var inntektsmelding = InntektsmeldingEntitet.builder()
            .medArbeidsgiverIdent("999999999")
            .medStartDato(LocalDate.of(2024, 6, 1))
            .medYtelsetype(Ytelsetype.FORELDREPENGER)
            .medMånedInntekt(BigDecimal.valueOf(35000))
            .medAktørId(aktørIdSøker)
            .medKontaktperson(new KontaktpersonEntitet("Test Testen", "111111111"))
            .medNaturalYtelse(Collections.singletonList(naturalytelse))
            .medRefusjonsPeriode(Collections.singletonList(refusjonperiode))
            .build();

        when(personTjeneste.finnPersonIdentForAktørId(aktørIdSøker)).thenReturn(fnrSøker);

        // Act
        var xml = inntektsmeldingXMLTjeneste.lagXMLAvInntektsmelding(inntektsmelding);

        // Assert
        // Kjører replace slik at vi kan lagre XML i mer lesbart format under
        assertThat(xml).isEqualTo(forventetXml().replaceAll("[\r\n\t]", ""));
    }

    private String forventetXml() {
        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <melding xmlns="http://seres.no/xsd/NAV/Inntektsmelding_M/20181211">
            	<Skjemainnhold>
            		<ytelse>Foreldrepenger</ytelse>
            		<aarsakTilInnsending>Ny</aarsakTilInnsending>
            		<arbeidsgiver>
            			<virksomhetsnummer>999999999</virksomhetsnummer>
            			<kontaktinformasjon>
            				<kontaktinformasjonNavn>Test Testen</kontaktinformasjonNavn>
            				<telefonnummer>111111111</telefonnummer>
            			</kontaktinformasjon>
            		</arbeidsgiver>
            		<arbeidstakerFnr>11111111111</arbeidstakerFnr>
            		<naerRelasjon>false</naerRelasjon>
            		<arbeidsforhold>
            			<startdatoForeldrepengeperiode>2024-06-01</startdatoForeldrepengeperiode>
            			<beregnetInntekt>
            				<beloep>35000</beloep>
            			</beregnetInntekt>
            		</arbeidsforhold>
            		<refusjon>
            			<refusjonsbeloepPrMnd>35000</refusjonsbeloepPrMnd>
            			<refusjonsopphoersdato>9999-12-31</refusjonsopphoersdato>
            			<endringIRefusjonListe/>
            		</refusjon>
            		<opphoerAvNaturalytelseListe>
            			<opphoerAvNaturalytelse>
            				<naturalytelseType>aksjerGrunnfondsbevisTilUnderkurs</naturalytelseType>
            				<fom>2024-06-10</fom>
            				<beloepPrMnd>2000</beloepPrMnd>
            			</opphoerAvNaturalytelse>
            		</opphoerAvNaturalytelseListe>
            		<gjenopptakelseNaturalytelseListe/>
            	</Skjemainnhold>
            </melding>
            """;
    }
}
