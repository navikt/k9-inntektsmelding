package no.nav.familie.inntektsmelding.server;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.jetty.server.Server;
import org.glassfish.jersey.server.model.Resource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Starter Jetty med prod-oppsettet fra {@link JettyServer#lagServletContext()} og kaller alle REST-endepunkter uten gyldig token.
 * Endepunktene finnes fra JAX-RS-applikasjonene som er registrert som servlets, så nye applikasjoner og endepunkter
 * testes automatisk. Åpne endepunkter må stå i {@link #ÅPNE_STIER}.
 * Manglende token gir i dag 403: AuthenticationFilterDelegate kaster ManglerTilgangException, som
 * GeneralRestExceptionMapper mapper til 403. Testen godtar derfor både 401 og 403.
 */
class RestApiUtenGyldigTokenAvvisesTest {

    // Stiprefiks -> begrunnelse for at endepunktene kan kalles uten token
    private static final Map<String, String> ÅPNE_STIER = Map.of(
        "/internal/", "Helse og metrikker for Nais. InternalApiConfig har ikke AutentiseringFilter.");

    private static final Set<Integer> AVVIST = Set.of(401, 403);
    private static final String JWT_FRA_UKJENT_UTSTEDER = jwt("{\"alg\":\"RS256\",\"typ\":\"JWT\"}",
        "{\"iss\":\"https://ukjent-utsteder\",\"sub\":\"test\",\"aud\":\"test\",\"exp\":4102444800}");

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static Server server;
    private static String baseUrl;

    record Endepunkt(String httpMetode, String sti, MediaType consumes) {
        @Override
        public String toString() {
            return httpMetode + " " + sti;
        }
    }

    @BeforeAll
    static void startServer() throws Exception {
        server = new Server(0);
        var context = JettyServer.lagServletContext();
        server.setHandler(context);
        server.start();
        baseUrl = server.getURI().resolve(context.getContextPath()).toString();
    }

    @AfterAll
    static void stoppServer() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void skal_finne_endepunkter_som_krever_autentisering() throws Exception {
        assertThat(endepunkter()).isNotEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkter")
    void skal_avvise_kall_uten_token(Endepunkt endepunkt) throws Exception {
        var response = kall(endepunkt, null);

        assertAvvist(endepunkt, response, "uten token");
    }

    // Signaturen sjekkes bare for utstedere appen kjenner, og testmiljøet har ingen. JWT-en stoppes derfor på ukjent utsteder.
    // En token som ikke er en JWT gir 500 (TekniskException fra felles-auth-filter), og er derfor ikke testet.
    @ParameterizedTest(name = "{0}")
    @MethodSource("endepunkter")
    void skal_avvise_kall_med_token_fra_ukjent_utsteder(Endepunkt endepunkt) throws Exception {
        var response = kall(endepunkt, JWT_FRA_UKJENT_UTSTEDER);

        assertAvvist(endepunkt, response, "token fra ukjent utsteder");
    }

    private static HttpResponse<String> kall(Endepunkt endepunkt, String token) throws Exception {
        var body = Set.of("POST", "PUT", "PATCH").contains(endepunkt.httpMetode())
            ? HttpRequest.BodyPublishers.ofString("{}")
            : HttpRequest.BodyPublishers.noBody();
        var request = HttpRequest.newBuilder(URI.create(baseUrl + endepunkt.sti()))
            .header("Content-Type", endepunkt.consumes().toString())
            .method(endepunkt.httpMetode(), body);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return HTTP_CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static void assertAvvist(Endepunkt endepunkt, HttpResponse<String> response, String beskrivelse) {
        assertThat(response.statusCode())
            .withFailMessage("%s ga %s med %s, forventet 401 eller 403. Er endepunktet ment å være åpent, legg det i ÅPNE_STIER. Body: %s",
                endepunkt, response.statusCode(), beskrivelse, response.body())
            .isIn(AVVIST);
    }

    private static String jwt(String header, String payload) {
        var encoder = Base64.getUrlEncoder().withoutPadding();
        return encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
            + encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "."
            + encoder.encodeToString("falsk-signatur".getBytes(StandardCharsets.UTF_8));
    }

    static List<Endepunkt> endepunkter() throws Exception {
        var servletHandler = JettyServer.lagServletContext().getServletHandler();
        var stiPerServlet = new HashMap<String, String>();
        for (var mapping : servletHandler.getServletMappings()) {
            for (var pathSpec : mapping.getPathSpecs()) {
                stiPerServlet.put(mapping.getServletName(), pathSpec.replaceFirst("/\\*$", ""));
            }
        }

        var endepunkter = new ArrayList<Endepunkt>();
        for (var servlet : servletHandler.getServlets()) {
            var applikasjonsklasse = servlet.getInitParameter(JettyServer.APPLICATION);
            if (applikasjonsklasse == null) {
                continue;
            }
            var applikasjon = (Application) Class.forName(applikasjonsklasse).getDeclaredConstructor().newInstance();
            var apiUri = stiPerServlet.get(servlet.getName());
            applikasjon.getClasses().stream()
                .filter(klasse -> klasse.isAnnotationPresent(Path.class))
                .forEach(klasse -> leggTilEndepunkter(endepunkter, apiUri, Resource.from(klasse)));
        }
        return endepunkter.stream()
            .filter(e -> ÅPNE_STIER.keySet().stream().noneMatch(åpen -> (e.sti() + "/").startsWith(åpen)))
            .toList();
    }

    private static void leggTilEndepunkter(List<Endepunkt> endepunkter, String forelderSti, Resource resource) {
        var sti = forelderSti + "/" + resource.getPath();
        for (var metode : resource.getResourceMethods()) {
            var consumes = metode.getConsumedTypes().stream()
                .filter(type -> !type.isWildcardType())
                .findFirst()
                .orElse(MediaType.APPLICATION_JSON_TYPE);
            endepunkter.add(new Endepunkt(metode.getHttpMethod(), normaliser(sti), consumes));
        }
        resource.getChildResources().forEach(barn -> leggTilEndepunkter(endepunkter, sti, barn));
    }

    // Erstatter stiparametre som {id} med en dummyverdi. Autentisering skjer før parametrene leses.
    private static String normaliser(String sti) {
        var normalisert = sti.replaceAll("\\{[^}]+}", "1").replaceAll("/+", "/");
        return normalisert.length() > 1 && normalisert.endsWith("/") ? normalisert.substring(0, normalisert.length() - 1) : normalisert;
    }
}
