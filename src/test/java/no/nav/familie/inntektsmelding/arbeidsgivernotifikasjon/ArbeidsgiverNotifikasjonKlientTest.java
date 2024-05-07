package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLError;
import com.kobylynskyi.graphql.codegen.model.graphql.GraphQLErrorType;

import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.rest.RestClient;
import no.nav.vedtak.felles.integrasjon.rest.RestRequest;

@ExtendWith(MockitoExtension.class)
class ArbeidsgiverNotifikasjonKlientTest {

    @Mock
    RestClient klient;

    private ArbeidsgiverNotifikasjonKlient tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new ArbeidsgiverNotifikasjonKlient(klient);
    }

    @Nested
    class OpprettSak {
        @Test
        void opprettSak_ok() {
            var response = new NySakMutationResponse();
            var expectedId = "12345";
            response.setData(Map.of("nySak", new NySakVellykket(expectedId)));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var oppgave = tjeneste.opprettSak(new NySakMutationRequest(), mock(NySakResultatResponseProjection.class));

            assertThat(oppgave).isNotNull().isEqualTo(expectedId);
        }

        @Test
        void opprettSak_validering_feil() {
            var expectedFeilmelding = "Det har skjedd en ny feil.";
            var response = new NySakMutationResponse();
            response.setData(Map.of("nySak", new UgyldigMerkelapp(expectedFeilmelding)));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var request = new NySakMutationRequest();
            var ex = assertThrows(TekniskException.class, () -> tjeneste.opprettSak(request, mock(NySakResultatResponseProjection.class)));

            assertThat(ex.getMessage()).contains(expectedFeilmelding);
        }

        @Test
        void opprettSak_teknisk_feil() {
            var expectedFeilmelding = "Det har skjedd en teknisk feil.";
            var response = new NySakMutationResponse();
            response.setErrors(
                List.of(new GraphQLError(expectedFeilmelding, List.of(), GraphQLErrorType.OperationNotSupported, List.of(), Map.of())));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var request = new NySakMutationRequest();
            var ex = assertThrows(TekniskException.class, () -> tjeneste.opprettSak(request, mock(NySakResultatResponseProjection.class)));

            assertThat(ex.getMessage()).contains(expectedFeilmelding);
        }
    }

    @Nested
    class OpprettOppgave {

        @Test
        void opprettOppgave_ok() {
            var response = new NyOppgaveMutationResponse();
            var expectedId = "12345";
            response.setData(Map.of("nyOppgave", new NyOppgaveVellykket(null, expectedId, null)));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var oppgave = tjeneste.opprettOppgave(new NyOppgaveMutationRequest(), mock(NyOppgaveResultatResponseProjection.class));

            assertThat(oppgave).isNotNull().isEqualTo(expectedId);
        }

        @Test
        void opprettOppgave_validering_feil() {
            var expectedFeilmelding = "Det har skjedd en ny feil.";
            var response = new NyOppgaveMutationResponse();
            response.setData(Map.of("nyOppgave", new UgyldigMerkelapp(expectedFeilmelding)));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var request = new NyOppgaveMutationRequest();
            var ex = assertThrows(TekniskException.class, () -> tjeneste.opprettOppgave(request, mock(NyOppgaveResultatResponseProjection.class)));

            assertThat(ex.getMessage()).contains(expectedFeilmelding);
        }

        @Test
        void opprettOppgave_teknisk_feil() {
            var expectedFeilmelding = "Det har skjedd en teknisk feil.";
            var response = new NyOppgaveMutationResponse();
            response.setErrors(
                List.of(new GraphQLError(expectedFeilmelding, List.of(), GraphQLErrorType.OperationNotSupported, List.of(), Map.of())));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var request = new NyOppgaveMutationRequest();
            var ex = assertThrows(TekniskException.class, () -> tjeneste.opprettOppgave(request, mock(NyOppgaveResultatResponseProjection.class)));

            assertThat(ex.getMessage()).contains(expectedFeilmelding);
        }
    }

    @Nested
    class LukkOppgave {

        @Test
        void lukkOppgave_ok() {
            var response = new OppgaveUtfoertMutationResponse();
            var expectedId = "12345";
            response.setData(Map.of("oppgaveUtfoert", new OppgaveUtfoertVellykket(expectedId)));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var oppgave = tjeneste.lukkOppgave(new OppgaveUtfoertMutationRequest(), mock(OppgaveUtfoertResultatResponseProjection.class));

            assertThat(oppgave).isNotNull().isEqualTo(expectedId);
        }

        @Test
        void lukkOppgave_validering_feil() {
            var expectedFeilmelding = "Det har skjedd en ny feil.";
            var response = new OppgaveUtfoertMutationResponse();
            response.setData(Map.of("oppgaveUtfoert", new NotifikasjonFinnesIkke(expectedFeilmelding)));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var request = new OppgaveUtfoertMutationRequest();
            var ex = assertThrows(TekniskException.class, () -> tjeneste.lukkOppgave(request, mock(OppgaveUtfoertResultatResponseProjection.class)));

            assertThat(ex.getMessage()).contains(expectedFeilmelding);
        }

        @Test
        void lukkOppgave_teknisk_feil() {
            var expectedFeilmelding = "Det har skjedd en teknisk feil.";
            var response = new OppgaveUtfoertMutationResponse();
            response.setErrors(
                List.of(new GraphQLError(expectedFeilmelding, List.of(), GraphQLErrorType.OperationNotSupported, List.of(), Map.of())));
            when(klient.send(any(RestRequest.class), any())).thenReturn(response);

            var request = new OppgaveUtfoertMutationRequest();
            var ex = assertThrows(TekniskException.class, () -> tjeneste.lukkOppgave(request, mock(OppgaveUtfoertResultatResponseProjection.class)));

            assertThat(ex.getMessage()).contains(expectedFeilmelding);
        }
    }
}
