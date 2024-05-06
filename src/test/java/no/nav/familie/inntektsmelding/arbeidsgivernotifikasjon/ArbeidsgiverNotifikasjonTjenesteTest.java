package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import static no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonTjeneste.SERVICE_CODE;
import static no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.ArbeidsgiverNotifikasjonTjeneste.SERVICE_EDITION_CODE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.net.URI;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLResponseProjection;

@ExtendWith(MockitoExtension.class)
class ArbeidsgiverNotifikasjonTjenesteTest {

    @Mock
    ArbeidsgiverNotifikasjonKlient klient;

    @Captor
    ArgumentCaptor<NyOppgaveMutationRequest> requestCaptor;

    private ArbeidsgiverNotifikasjon tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new ArbeidsgiverNotifikasjonTjeneste(klient);
    }

    @Test
    @Disabled
    void testKallToFager() {

        var expectedEksternId = "TestId";
        var expectedVirksomhetsnummer = "2342342334";
        var expectedTekst = "Ble";
        var expectedLenke = "http://haha.com";
        var expectedMerkelapp = Merkelapp.INNTEKTSMELDING_FP;
        var expectedTidspunkt = LocalDateTime.now();

        tjeneste.opprettNyOppgave(expectedEksternId, expectedTekst, URI.create(expectedLenke), expectedMerkelapp, expectedVirksomhetsnummer, expectedTidspunkt);

        Mockito.verify(klient).opprettNyOppgave(requestCaptor.capture(), any(GraphQLResponseProjection.class));

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
        assertThat(nyOppgave.getMetadata().getGrupperingsid()).isNull();
        assertThat(nyOppgave.getMetadata().getVirksomhetsnummer()).isNotNull().isEqualTo(expectedVirksomhetsnummer);
        assertThat(nyOppgave.getMetadata().getOpprettetTidspunkt()).isNotNull().isEqualTo(expectedTidspunkt.toString());
        assertThat(nyOppgave.getNotifikasjon()).isNotNull();
        assertThat(nyOppgave.getNotifikasjon().getTekst()).isEqualTo(expectedTekst);
        assertThat(nyOppgave.getNotifikasjon().getLenke()).isEqualTo(expectedLenke);
        assertThat(nyOppgave.getNotifikasjon().getMerkelapp()).isEqualTo(expectedMerkelapp.getBeskrivelse());
        assertThat(nyOppgave.getFrist()).isNull();
        assertThat(nyOppgave.getMottakere()).isEmpty();
        assertThat(nyOppgave.getPaaminnelse()).isNull();
    }
}
