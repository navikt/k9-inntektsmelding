package no.nav.familie.inntektsmelding.server.app.internal.rest;

import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.server.ApplicationServiceStarter;
import no.nav.vedtak.log.metrics.LivenessAware;
import no.nav.vedtak.log.metrics.ReadinessAware;

@Path("/health")
@ApplicationScoped
public class HealtCheckRest {

    private static final Logger LOG = LoggerFactory.getLogger(HealtCheckRest.class);
    private static final CacheControl cacheControl = noCache();

    private List<LivenessAware> live;
    private List<ReadinessAware> ready;
    private ApplicationServiceStarter starter;

    private static CacheControl noCache() {
        var cc = new CacheControl();
        cc.setMustRevalidate(true);
        cc.setPrivate(true);
        cc.setNoCache(true);
        cc.setNoStore(true);
        return cc;
    }

    @Inject
    public HealtCheckRest(ApplicationServiceStarter starter, @Any Instance<LivenessAware> live, @Any Instance<ReadinessAware> ready) {
        this.live = live.stream().toList();
        this.ready = ready.stream().toList();
        this.starter = starter;
    }

    HealtCheckRest() {
        //CDI
    }

    @GET
    @Path("/isAlive")
    public Response isAlive() {
        Response.ResponseBuilder builder;
        if (live.stream().allMatch(LivenessAware::isAlive)) {
            builder = Response.ok("OK", MediaType.TEXT_PLAIN_TYPE);
        } else {
            builder = Response.serverError();
        }
        LOG.info("/isAlive NOK.");
        return builder.cacheControl(cacheControl).build();
    }

    @GET
    @Path("/isReady")
    public Response isReady() {
        Response.ResponseBuilder builder;
        if (ready.stream().allMatch(ReadinessAware::isReady)) {
            builder = Response.ok("OK", MediaType.TEXT_PLAIN_TYPE);
        } else {
            builder = Response.status(SERVICE_UNAVAILABLE);
        }
        LOG.info("/isReady NOK.");
        return builder.cacheControl(cacheControl).build();
    }

    @GET
    @Path("/preStop")
    public Response preStop() {
        starter.stopServices();
        return Response.ok().build();
    }
}
