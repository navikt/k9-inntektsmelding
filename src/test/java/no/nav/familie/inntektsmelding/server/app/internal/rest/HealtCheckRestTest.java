package no.nav.familie.inntektsmelding.server.app.internal.rest;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import no.nav.familie.inntektsmelding.server.ApplicationServiceStarter;

import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class HealtCheckRestTest {

    HealtCheckRest hcr = new HealtCheckRest(mock(ApplicationServiceStarter.class) ,
        mock(Instance.class), mock(Instance.class));

    @Test
    public void skalBrukeCacheControlForAlivesjekk() {
        Response response = hcr.isAlive();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("private, no-cache, no-store, no-transform, must-revalidate", response.getHeaderString(HttpHeaders.CACHE_CONTROL));
    }
    @Test
    public void skalBrukeCacheControlForReadinessjekk() {
        Response response = hcr.isAlive();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("private, no-cache, no-store, no-transform, must-revalidate", response.getHeaderString(HttpHeaders.CACHE_CONTROL));
    }
}
