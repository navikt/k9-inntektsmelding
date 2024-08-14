package no.nav.familie.inntektsmelding.server.openapi;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.*;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;

public class OpenApiRest extends OpenApiResource {
    @Context
    ServletConfig config;

    @Context
    Application app;

    @GET
    @Produces({MediaType.APPLICATION_JSON, "application/yaml"})
    @Operation(hidden = true)
    @AutentisertMedAzure
    public Response getOpenApi(@Context HttpHeaders headers,
                               @Context UriInfo uriInfo,
                               @PathParam("type") String type) throws Exception {

        return super.getOpenApi(headers, uriInfo, type);
    }

}
