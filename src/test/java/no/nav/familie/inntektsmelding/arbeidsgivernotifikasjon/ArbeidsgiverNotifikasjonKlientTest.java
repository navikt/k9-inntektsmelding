package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import static no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon.NyOppgave.VELLYKET_TYPENAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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

    @Captor
    ArgumentCaptor<NyOppgaveMutationRequest> requestCaptor;

    private ArbeidsgiverNotifikasjonKlient tjeneste;

    @BeforeEach
    void setUp() {
        tjeneste = new ArbeidsgiverNotifikasjonKlient(klient);
    }

    @Test
    void opprettNyOppgave_ok() {
        var response = new NyOppgaveResponse();
        var expectedId = "12345";
        response.setData(Map.of("nyOppgave", new NyOppgave(VELLYKET_TYPENAME, expectedId, null)));
        when(klient.send(any(RestRequest.class), any())).thenReturn(response);

        var oppgave = tjeneste.opprettNyOppgave(new NyOppgaveMutationRequest(), mock(NyOppgaveResultatResponseProjection.class));

        assertThat(oppgave).isNotNull().isEqualTo(expectedId);
    }

    @Test
    void opprettNyOppgave_velidering_feil() {
        var expectedTypename = "NoeFeil";
        var expectedFeilmelding = "Det har skjed en ny feil.";
        var response = new NyOppgaveResponse();
        response.setData(Map.of("nyOppgave", new NyOppgave(expectedTypename, null, expectedFeilmelding)));
        when(klient.send(any(RestRequest.class), any())).thenReturn(response);

        var request = new NyOppgaveMutationRequest();
        var ex = assertThrows(TekniskException.class,
            () -> tjeneste.opprettNyOppgave(request, mock(NyOppgaveResultatResponseProjection.class)));

        assertThat(ex.getMessage()).contains(expectedTypename, expectedFeilmelding);
    }

    @Test
    void opprettNyOppgave_teknisk_feil() {
        var expectedFeilmelding = "Det har skjed en teknisk feil.";
        var response = new NyOppgaveResponse();
        response.setErrors(List.of(new GraphQLError(expectedFeilmelding, List.of(), GraphQLErrorType.OperationNotSupported, List.of(), Map.of())));
        when(klient.send(any(RestRequest.class), any())).thenReturn(response);

        var request = new NyOppgaveMutationRequest();
        var ex = assertThrows(TekniskException.class,
            () -> tjeneste.opprettNyOppgave(request, mock(NyOppgaveResultatResponseProjection.class)));

        assertThat(ex.getMessage()).contains(expectedFeilmelding);
    }
}
