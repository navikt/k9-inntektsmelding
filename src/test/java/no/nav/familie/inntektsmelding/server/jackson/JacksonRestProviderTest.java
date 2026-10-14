package no.nav.familie.inntektsmelding.server.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import no.nav.familie.inntektsmelding.server.exceptions.JsonMappingExceptionMapper;
import no.nav.familie.inntektsmelding.server.exceptions.JsonParseExceptionMapper;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

class JacksonRestProviderTest {

    private static ApplicationHandler handler;

    record EkkoDto(String navn, LocalDate dato, List<String> liste) {
    }

    @Path("/ekko")
    public static class EkkoRest {
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public EkkoDto ekko(EkkoDto dto) {
            return dto;
        }
    }

    @BeforeAll
    static void setUp() {
        var config = new ResourceConfig()
            .register(EkkoRest.class)
            .register(JsonMappingExceptionMapper.class)
            .register(JsonParseExceptionMapper.class)
            .register(JacksonJsonProvider.class)
            .register(JacksonJsonConfig.class);
        handler = new ApplicationHandler(config);
    }

    @Test
    void skal_bruke_jackson3_provider_med_vaar_konfigurasjon() throws Exception {
        var respons = post("{\"navn\":\"navn\",\"dato\":\"2026-01-31\",\"liste\":[],\"ukjent\":true}");

        assertThat(respons.status()).isEqualTo(200);
        assertThat(respons.body()).isEqualTo("{\"navn\":\"navn\",\"dato\":\"2026-01-31\"}");
    }

    @Test
    void skal_mappe_parse_feil_til_400() throws Exception {
        var respons = post("{\"navn\":");

        assertThat(respons.status()).isEqualTo(400);
        assertThat(respons.body()).contains("FIM-299955");
    }

    @Test
    void skal_mappe_mapping_feil_til_400() throws Exception {
        var respons = post("{\"dato\":\"ikke-en-dato\"}");

        assertThat(respons.status()).isEqualTo(400);
        assertThat(respons.body()).contains("FIM-252294");
    }

    private record Respons(int status, String body) {
    }

    private static Respons post(String json) throws Exception {
        var request = new ContainerRequest(URI.create("http://localhost/"), URI.create("http://localhost/ekko"), "POST", null,
            new MapPropertiesDelegate(), handler.getConfiguration());
        request.getHeaders().putSingle("Content-Type", MediaType.APPLICATION_JSON);
        request.getHeaders().putSingle("Accept", MediaType.APPLICATION_JSON);
        request.setEntityStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
        var out = new ByteArrayOutputStream();
        request.setWriter(new ContainerResponseWriter() {
            @Override
            public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext) {
                return out;
            }

            @Override
            public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
                return false;
            }

            @Override
            public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) {
            }

            @Override
            public void commit() {
            }

            @Override
            public void failure(Throwable error) {
            }

            @Override
            public boolean enableResponseBuffering() {
                return false;
            }
        });
        var response = handler.apply(request, out).get();
        return new Respons(response.getStatus(), out.toString(StandardCharsets.UTF_8));
    }
}
