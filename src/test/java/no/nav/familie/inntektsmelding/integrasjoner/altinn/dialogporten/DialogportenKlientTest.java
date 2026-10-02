package no.nav.familie.inntektsmelding.integrasjoner.altinn.dialogporten;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;

import no.nav.vedtak.exception.IntegrasjonException;

class DialogportenKlientTest {

    private static final String UKJENT_AKTØR_RESPONSBODY =
        "{\"errors\":{\"ActorId\":[\"Unable to look up name for actor id: urn:altinn:organization:identifier-no:967170232\"]},"
            + "\"type\":\"https://datatracker.ietf.org/doc/html/rfc4918#section-11.2\",\"title\":\"Unprocessable request.\",\"status\":422}";

    @Test
    void skal_gjenkjenne_ukjent_aktør_feil_ved_422_og_forventet_feiltekst() {
        var response = mockResponse(422, UKJENT_AKTØR_RESPONSBODY);

        assertThat(DialogportenKlient.erUkjentAktørFeil(response)).isTrue();
    }

    @Test
    void skal_ikke_gjenkjenne_annen_422_feil_som_ukjent_aktør() {
        var response = mockResponse(422, "{\"title\":\"En annen valideringsfeil\"}");

        assertThat(DialogportenKlient.erUkjentAktørFeil(response)).isFalse();
    }

    @Test
    void skal_ikke_gjenkjenne_ukjent_aktør_feiltekst_på_annen_statuskode() {
        var response = mockResponse(500, UKJENT_AKTØR_RESPONSBODY);

        assertThat(DialogportenKlient.erUkjentAktørFeil(response)).isFalse();
    }

    @Test
    void skal_svelge_ukjent_aktør_feil_når_flagg_er_satt() throws Exception {
        var response = mockResponse(422, UKJENT_AKTØR_RESPONSBODY);

        var resultat = invokeHandleResponse(response, true);

        assertThat(resultat).isNull();
    }

    @Test
    void skal_kaste_exception_for_ukjent_aktør_feil_når_flagg_ikke_er_satt() {
        var response = mockResponse(422, UKJENT_AKTØR_RESPONSBODY);

        assertThatThrownBy(() -> invokeHandleResponse(response, false))
            .cause()
            .isInstanceOf(IntegrasjonException.class);
    }

    @Test
    void skal_kaste_exception_for_annen_feil_selv_om_flagg_er_satt() {
        var response = mockResponse(500, "{\"title\":\"Serverfeil\"}");

        assertThatThrownBy(() -> invokeHandleResponse(response, true))
            .cause()
            .isInstanceOf(IntegrasjonException.class);
    }

    @Test
    void skal_returnere_body_ved_ok_respons() throws Exception {
        var response = mockResponse(200, "{\"id\":\"abc\"}");

        var resultat = invokeHandleResponse(response, false);

        assertThat(resultat).isEqualTo("{\"id\":\"abc\"}");
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> mockResponse(int statusCode, String body) {
        var response = (HttpResponse<String>) mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }

    private static String invokeHandleResponse(HttpResponse<String> response, boolean ignorerUkjentAktørFeil) throws Exception {
        var klient = mock(DialogportenKlient.class);
        Method method = DialogportenKlient.class.getDeclaredMethod("handleResponse", HttpResponse.class, boolean.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(klient, response, ignorerUkjentAktørFeil);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
    }
}

