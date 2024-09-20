package no.nav.familie.inntektsmelding.server.authz;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.pip.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.pip.PipTjeneste;
import no.nav.familie.inntektsmelding.server.authz.api.ActionType;
import no.nav.familie.inntektsmelding.server.authz.api.PolicyType;
import no.nav.familie.inntektsmelding.server.authz.api.Tilgangsstyring;
import no.nav.familie.inntektsmelding.server.authz.api.TilgangsstyringDto;
import no.nav.familie.inntektsmelding.server.authz.api.TilgangsstyringInput;
import no.nav.familie.inntektsmelding.server.authz.api.TilgangsstyringInputSupplier;
import no.nav.familie.inntektsmelding.server.authz.api.exception.NektetTilgangException;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.foreldrepenger.konfig.Cluster;
import no.nav.foreldrepenger.konfig.Environment;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.AzureProperty;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;

@RequestScoped
@Provider
@Priority(Priorities.AUTHORIZATION)
public class TilgangsstyringFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TilgangsstyringFilter.class);
    private static final Logger SECURE_LOG = LoggerFactory.getLogger("secureLogger");
    private static final Environment ENV = Environment.current();

    @Context
    private ResourceInfo resourceinfo;

    private PipTjeneste pipTjeneste;
    private AltinnTilgangTjeneste tilgangTjeneste;

    private String preAuthorized;
    private Cluster residentCluster;
    private String residentNamespace;

    @Inject
    public TilgangsstyringFilter(PipTjeneste pipTjeneste, AltinnTilgangTjeneste tilgangTjeneste) {
        this.pipTjeneste = pipTjeneste;
        this.tilgangTjeneste = tilgangTjeneste;
        this.preAuthorized = ENV.getProperty(AzureProperty.AZURE_APP_PRE_AUTHORIZED_APPS.name());
        this.residentCluster = ENV.getCluster();
        this.residentNamespace = ENV.namespace();
    }

    TilgangsstyringFilter() {
        // CDI proxy
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        // Her bør det ligge auditlogging om det blir behov for intern bruker tilgang.
    }

    @Override
    public void filter(ContainerRequestContext req) {
        var method = resourceinfo.getResourceMethod();

        if (ENV.isLocal() && method.getAnnotation(UtenAutentisering.class) != null) {
            LOG.trace("{} er whitelisted", method);
            return;
        }

        if (!KontekstHolder.harKontekst()) {
            throw new TekniskException("FP-234584", "Kontekst må være satt");
        }

        var annotation = method.getAnnotation(Tilgangsstyring.class);
        if (annotation == null) {
            throw new TekniskException("FP-234584", "Ressurs mangler riktig annotering");
        }

        var kontekst = KontekstHolder.getKontekst();
        var policy = annotation.policy();
        var action = annotation.action();

        // her kjøres selve reglene - tidligere kall til PDP/ABAC - mulig å lage et rammeverk rundt dette - men var overkill til å gjøre dette her.
        var tilgangBeslutning = switch (kontekst.getIdentType()) {
            case EksternBruker -> vurderBorgerTilgang(policy, action);
            case Systemressurs -> vurderSystemTilgang(policy, action);
            case null, default -> ikkeTilgang(String.format("Mangler policy for %s", kontekst.getIdentType()));
        };

        if (!tilgangBeslutning.fikkTilgang()) {
            throw new NektetTilgangException("IKKE-TILGANG", tilgangBeslutning.begrunnelse());
        } else {
            LOG.info("Tilgang ok.");
        }
    }

    private Tilgangsbeslutning vurderSystemTilgang(PolicyType policy, ActionType action) {
        var kontekst = KontekstHolder.getKontekst();
        if (!erAzureProvider(kontekst)) {
            return ikkeTilgang("Kun Azure brukere støttes.");
        }

        if (PolicyType.ARBEIDSGIVER_PORTAL.equals(policy)) {
            if (ActionType.WRITE.equals(action)) {
                var konsumentUid = kontekst.getUid();
                if (erPreautorisert(konsumentUid) && erISammeKlusterKlasseOgNamespace(konsumentUid)) {
                    return okTilgang();
                }
                SECURE_LOG.warn("Konsumenten {} er ikke autorisert", konsumentUid);
                return ikkeTilgang("Konsumenten er ikke autorisert.");
            }
        }
        LOG.warn("Mangler policy for {}, action {}", policy, action);
        return ikkeTilgang("Mangler policy.");
    }

    private Tilgangsbeslutning vurderBorgerTilgang(PolicyType policy, ActionType action) {
        var kontekst = KontekstHolder.getKontekst();
        if (!erTokenXProvider(kontekst)) {
            return ikkeTilgang("Kun TokenX brukere støttes.");
        }

        var input = finnTilgangsstyringInput();

        if (PolicyType.ARBEIDSGIVER.equals(policy)) {
            if (Set.of(ActionType.READ, ActionType.WRITE).contains(action)) {

                //            TODO: Avklar om vi må sjekke om AG representanten kan sende inn en IM for seg selv. Mulig har kan lese data om segselv
                //                  men bør ikke kunne sende inn.
                //                if (!kontekst.getUid().equals(input.getVerdier(TilgangsstyringInputTyper.AKTØR_ID)) {
                //                    return ikkeTilgang("Brukeren kan ikke sende inn for seg selv.");
                //                }

                var orgNrSet = input.getVerdier(TilgangsstyringInputTyper.FORESPORSEL_ID)
                    .stream()
                    .map(uuid -> pipTjeneste.hentOrganisasjonsnummerFor((UUID) uuid))
                    .map(OrganisasjonsnummerDto::orgnr)
                    .collect(Collectors.toSet());

                if (orgNrSet.isEmpty()) {
                    return ikkeTilgang("Mangler informasjon om bedrift.");
                }

                for (var orgNr : orgNrSet) {
                    if (tilgangTjeneste.manglerTilgangTilBedriften(orgNr)) {
                        SECURE_LOG.warn("Bruker {} mangler tilgang til bedrift {}", kontekst.getUid(), orgNr);
                        return ikkeTilgang("Mangler tilgang til bedrift.");
                    }
                }
                return okTilgang();
            }
        }
        LOG.warn("Mangler policy for {}, action {}", policy, action);
        return ikkeTilgang("Mangler policy.");
    }

    private TilgangsstyringInput finnTilgangsstyringInput() {
        var method = resourceinfo.getResourceMethod();
        var parameterDecl = method.getParameters();
        var input = TilgangsstyringInput.opprett();
        for (int i = 0; i < method.getParameterCount(); i++) {
            Object parameterValue = method.getParameters()[i];
            var parameterAnnotering = parameterDecl[i].getAnnotation(TilgangsstyringInputSupplier.class);
            leggTilAttributterFraParameter(input, parameterValue, parameterAnnotering);
        }
        return input;
    }

    @SuppressWarnings("rawtypes")
    static void leggTilAttributterFraParameter(TilgangsstyringInput input, Object parameterValue, TilgangsstyringInputSupplier supplierAnnotering) {
        if (supplierAnnotering != null) {
            leggTil(input, supplierAnnotering, parameterValue);
        } else {
            if (parameterValue instanceof TilgangsstyringDto tilgangDto) {
                input.leggTil(tilgangDto.inputAttributer());
            } else if (parameterValue instanceof Collection collection) {
                leggTilTilgangsstyringDtoSamling(input, collection);
            }
        }
    }

    private static void leggTilTilgangsstyringDtoSamling(TilgangsstyringInput input, Collection<?> parameterValue) {
        for (var value : parameterValue) {
            if (value instanceof TilgangsstyringDto tilgangDto) {
                input.leggTil(tilgangDto.inputAttributer());
            } else {
                throw new TekniskException("F-261962",
                    String.format("Ugyldig input forventet at samling inneholdt bare TilgangsstyringDto-er, men fant %s",
                        value != null ? value.getClass().getName() : "null"));
            }
        }
    }

    private static void leggTil(TilgangsstyringInput input, TilgangsstyringInputSupplier supplierAnnotering, Object verdi) {
        try {
            var atributter = supplierAnnotering.value().getDeclaredConstructor().newInstance().apply(verdi);
            input.leggTil(atributter);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(e.getCause());
        }
    }

    private boolean erPreautorisert(String konsumentUid) {
        return konsumentUid != null && preAuthorized.contains(konsumentUid);
    }

    private boolean erISammeKlusterKlasseOgNamespace(String konsumentUid) {
        var elementer = konsumentUid.split(":");
        if (elementer.length < 2) {
            return false;
        }

        var consumerCluster = elementer[0];
        var consumerNamespace = elementer[1];
        return residentCluster.isSameClass(Cluster.of(consumerCluster)) && residentNamespace.equals(consumerNamespace);
    }

    private boolean erAzureProvider(Kontekst kontekst) {
        if (kontekst instanceof RequestKontekst requestKontekst) {
            var provider = requestKontekst.getToken().provider();
            return OpenIDProvider.AZUREAD.equals(provider);
        }
        return false;
    }

    private boolean erTokenXProvider(Kontekst kontekst) {
        if (kontekst instanceof RequestKontekst requestKontekst) {
            var provider = requestKontekst.getToken().provider();
            return OpenIDProvider.TOKENX.equals(provider);
        }
        return false;
    }

    private static Tilgangsbeslutning ikkeTilgang(String begrunnelse) {
        return new Tilgangsbeslutning(TilgangResultat.AVSLÅTT_ANNEN_ÅRSAK, begrunnelse);
    }

    private static Tilgangsbeslutning okTilgang() {
        return new Tilgangsbeslutning(TilgangResultat.GODKJENT, "OK");
    }

    record Tilgangsbeslutning(TilgangResultat beslutningKode, String begrunnelse) {
        public boolean fikkTilgang() {
            return beslutningKode == TilgangResultat.GODKJENT;
        }
    }

    enum TilgangResultat {
        GODKJENT,
        AVSLÅTT_ANNEN_ÅRSAK;
    }

}
