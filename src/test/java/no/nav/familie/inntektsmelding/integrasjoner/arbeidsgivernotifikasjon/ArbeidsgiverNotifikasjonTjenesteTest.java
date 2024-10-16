package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonTjeneste.SERVICE_CODE;
import static no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonTjeneste.SERVICE_EDITION_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ArbeidsgiverNotifikasjonTjenesteTest {

    @Mock
    ArbeidsgiverNotifikasjonKlient klient;

    private ArbeidsgiverNotifikasjon tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new ArbeidsgiverNotifikasjonTjeneste(klient);
    }

    @Test
    void opprett_sak() {

        var expectedGrupperingsid = "id-som-knytter-sak-til-notifikasjon";
        var expectedVirksomhetsnummer = "2342342334";
        var expectedTittel = "Inntektsmelding for person";
        var expectedLenke = "https://inntektsmelding-innsendings-dialog.com";
        var expectedMerkelapp = Merkelapp.INNTEKTSMELDING_FP;
        var expectedStatusTekst = "status tekst";

        var requestCaptor = ArgumentCaptor.forClass(NySakMutationRequest.class);

        tjeneste.opprettSak(expectedGrupperingsid, expectedMerkelapp, expectedVirksomhetsnummer, expectedTittel, URI.create(expectedLenke), expectedStatusTekst);

        Mockito.verify(klient).opprettSak(requestCaptor.capture(), any(NySakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        var input = request.getInput();
        assertThat(input).isNotNull().hasSize(8);
        assertThat(input.get("grupperingsid")).isEqualTo(expectedGrupperingsid);
        assertThat(input.get("initiellStatus")).isEqualTo(SaksStatus.UNDER_BEHANDLING);
        assertThat(input.get("lenke")).isEqualTo(expectedLenke);
        assertThat(input.get("merkelapp")).isEqualTo(expectedMerkelapp.getBeskrivelse());
        assertThat(input.get("tittel")).isEqualTo(expectedTittel);
        assertThat(input.get("virksomhetsnummer")).isEqualTo(expectedVirksomhetsnummer);
        assertThat(input.get("mottakere")).isNotNull();
        assertThat(input.get("overstyrStatustekstMed")).isEqualTo(expectedStatusTekst);
    }

    @Test
    void opprett_oppgave() {

        var expectedEksternId = "TestId";
        var expectedGrupperingsid = "id-som-knytter-sak-til-notifikasjon";
        var expectedVirksomhetsnummer = "2342342334";
        var expectedNotifikasjonsTekst = "Du har en ny oppgave i AG-portalen";
        var expectedEksternvarselTekst = "En ansatt har søkt foreldrepenger";
        var expectedNotifikasjonsLenke = "https://arbeidsgiver-portal.com";
        var expectedNotifikasjonsMerkelapp = Merkelapp.INNTEKTSMELDING_FP;

        var requestCaptor = ArgumentCaptor.forClass(NyOppgaveMutationRequest.class);

        tjeneste.opprettOppgave(expectedGrupperingsid, expectedNotifikasjonsMerkelapp, expectedEksternId, expectedVirksomhetsnummer,
            expectedNotifikasjonsTekst, expectedEksternvarselTekst, URI.create(expectedNotifikasjonsLenke));

        Mockito.verify(klient).opprettOppgave(requestCaptor.capture(), any(NyOppgaveResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        assertThat(request.getInput()).isNotNull().hasSize(1);
        var inputKey = "nyOppgave";
        assertThat(request.getInput()).containsKey(inputKey);
        assertThat(request.getInput().get(inputKey)).isInstanceOf(NyOppgaveInput.class);
        var nyOppgave = (NyOppgaveInput) request.getInput().get(inputKey);

        assertThat(nyOppgave.getMottaker()).isNotNull();
        assertThat(nyOppgave.getMottaker().getAltinn().getServiceCode()).isEqualTo(SERVICE_CODE);
        assertThat(nyOppgave.getMottaker().getAltinn().getServiceEdition()).isEqualTo(SERVICE_EDITION_CODE);

        assertThat(nyOppgave.getMetadata()).isNotNull();
        assertThat(nyOppgave.getMetadata().getEksternId()).isNotNull().isEqualTo(expectedEksternId);
        assertThat(nyOppgave.getMetadata().getGrupperingsid()).isNotNull().isEqualTo(expectedGrupperingsid);
        assertThat(nyOppgave.getMetadata().getVirksomhetsnummer()).isNotNull().isEqualTo(expectedVirksomhetsnummer);
        assertThat(nyOppgave.getMetadata().getOpprettetTidspunkt()).isNull();

        assertThat(nyOppgave.getNotifikasjon()).isNotNull();
        assertThat(nyOppgave.getNotifikasjon().getTekst()).isEqualTo(expectedNotifikasjonsTekst);
        assertThat(nyOppgave.getNotifikasjon().getLenke()).isEqualTo(expectedNotifikasjonsLenke);
        assertThat(nyOppgave.getNotifikasjon().getMerkelapp()).isEqualTo(expectedNotifikasjonsMerkelapp.getBeskrivelse());

        assertThat(nyOppgave.getEksterneVarsler()).hasSize(1);
        assertThat(nyOppgave.getEksterneVarsler().getFirst().getAltinntjeneste()).isNotNull();
        assertThat(nyOppgave.getEksterneVarsler().getFirst().getAltinntjeneste().getInnhold()).isEqualTo(expectedEksternvarselTekst);

        assertThat(nyOppgave.getFrist()).isNull();
        assertThat(nyOppgave.getMottakere()).isEmpty();
        assertThat(nyOppgave.getPaaminnelse()).isNull();
    }

    @Test
    void lukk_oppgave() {
        var expectedId = "TestId";
        var expectedTidspunkt = OffsetDateTime.now();

        var requestCaptor = ArgumentCaptor.forClass(OppgaveUtfoertMutationRequest.class);

        tjeneste.oppgaveUtført(expectedId, expectedTidspunkt);

        Mockito.verify(klient).oppgaveUtført(requestCaptor.capture(), any(OppgaveUtfoertResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        assertThat(request.getInput()).isNotNull().hasSize(2);
        assertThat(request.getInput().get("id")).isNotNull().isEqualTo(expectedId);
        assertThat(request.getInput().get("utfoertTidspunkt")).isNotNull().isEqualTo(expectedTidspunkt.format(DateTimeFormatter.ISO_DATE_TIME));
    }


    @Test
    void ferdigstill_sak() {
        var expectedId = "TestId";
        var expectedStatusText = "Saksbehandler har gått videre uten din inntektsmelding";

        var requestCaptor = ArgumentCaptor.forClass(NyStatusSakMutationRequest.class);

        tjeneste.ferdigstillSak(expectedId,expectedStatusText);

        Mockito.verify(klient).oppdaterSakStatus(requestCaptor.capture(), any(NyStatusSakResultatResponseProjection.class));

        var request = requestCaptor.getValue();

        assertThat(request.getInput()).isNotNull().hasSize(3);
        assertThat(request.getInput().get("id")).isNotNull().isEqualTo(expectedId);
        assertThat(request.getInput().get("overstyrStatustekstMed")).isNotNull().isEqualTo(expectedStatusText);
    }
}
