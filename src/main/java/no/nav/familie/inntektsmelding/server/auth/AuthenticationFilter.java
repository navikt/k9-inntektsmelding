package no.nav.familie.inntektsmelding.server.auth;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.container.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedAzure;
import no.nav.familie.inntektsmelding.server.auth.api.AutentisertMedTokenX;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.sikkerhet.jaxrs.AuthenticationFilterDelegate;
import no.nav.vedtak.sikkerhet.jaxrs.UtenAutentisering;
import no.nav.vedtak.sikkerhet.kontekst.BasisKontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;
import no.nav.vedtak.sikkerhet.oidc.config.ConfigProvider;
import no.nav.vedtak.sikkerhet.oidc.config.OpenIDProvider;
import no.nav.vedtak.sikkerhet.oidc.token.OpenIDToken;
import no.nav.vedtak.sikkerhet.oidc.token.TokenString;
import no.nav.vedtak.sikkerhet.oidc.validator.JwtUtil;
import no.nav.vedtak.sikkerhet.oidc.validator.OidcTokenValidatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AuthenticationFilter.class);
    private static final List<Class<? extends Annotation>> GYLDIGE_ANNOTERINGER = List.of(AutentisertMedAzure.class, AutentisertMedTokenX.class, UtenAutentisering.class);

    @Context
    private ResourceInfo resourceinfo;

    public AuthenticationFilter() {
        // Ingenting
    }

    @Override
    public void filter(ContainerRequestContext req, ContainerResponseContext res) {
        AuthenticationFilterDelegate.fjernKontekst();
    }

    @Override
    public void filter(ContainerRequestContext req) {
        var method = resourceinfo.getResourceMethod();
        assertValidRequest(method, req);
    }

    void assertValidRequest(Method method, ContainerRequestContext req) {
        fjernKontektsHvisFinnes();
        setCallAndConsumerId(req);
        LOG.trace("{} i klasse {}", method.getName(), method.getDeclaringClass());

        assertValidAnnotation(method, req);
    }

    private void assertValidAnnotation(Method method, ContainerRequestContext req) {
        var annotation = getAnnotation(method);
        LOG.debug("Annotering på {} -> {}", method.getName(), annotation);
        if (annotation != null) {
            assertValidAnnotation(annotation, req);
        } else {
            throw new WebApplicationException(String.format("Mangler gyldig annotering på %s.", method.getName()), Response.Status.FORBIDDEN);
        }
    }

    /**
     * Letter etter en gyldig annotering på methoden og så på klassen.
     * Annoteringen på methodenivå overstyrer annotering på klassenivå.
     *
     * @param method REST mothoden som kalles
     * @return funnet annotering.
     */
    private static Annotation getAnnotation(Method method) {
        return findAnnotation(method.getAnnotations()).or(() -> findAnnotation(method.getDeclaringClass().getAnnotations())).orElse(null);
    }

    private static Optional<Annotation> findAnnotation(Annotation[] annotations) {
        return Arrays.stream(annotations).filter(a -> GYLDIGE_ANNOTERINGER.contains(a.annotationType())).findFirst();
    }

    private void assertValidAnnotation(Annotation annotering, ContainerRequestContext req) {
        switch (annotering) {
            case UtenAutentisering ignored -> {
                LOG.warn("Åpen endepunkt '{}' uten autentisering.", req.getMethod());
                KontekstHolder.setKontekst(BasisKontekst.ikkeAutentisertRequest(MDCOperations.getConsumerId()));
            }
            case AutentisertMedAzure ignored -> validerToken(req, OpenIDProvider.AZUREAD);
            case AutentisertMedTokenX ignored -> validerToken(req, OpenIDProvider.TOKENX);
            case null, default -> throw new IllegalStateException("Utviklerfeil: Mangler annotering på endepunkt.");
        }
    }

    private void fjernKontektsHvisFinnes() {
        if (KontekstHolder.harKontekst()) {
            LOG.info("Kall til {} hadde kontekst {}", getResourceinfo().getResourceMethod().getName(), KontekstHolder.getKontekst().getKompaktUid());
            KontekstHolder.fjernKontekst();
            MDC.clear();
        }
    }

    private static void setCallAndConsumerId(ContainerRequestContext request) {
        String callId = Optional.ofNullable(request.getHeaderString(MDCOperations.HTTP_HEADER_CALL_ID)).or(() -> Optional.ofNullable(request.getHeaderString(MDCOperations.HTTP_HEADER_ALT_CALL_ID))).orElseGet(MDCOperations::generateCallId);
        MDCOperations.putCallId(callId);

        Optional.ofNullable(request.getHeaderString(MDCOperations.HTTP_HEADER_CONSUMER_ID)).ifPresent(MDCOperations::putConsumerId);
    }

    private static void setUserAndConsumerId(String subject) {
        Optional.ofNullable(subject).ifPresent(MDCOperations::putUserId);
        if (MDCOperations.getConsumerId() == null && subject != null) {
            MDCOperations.putConsumerId(subject);
        }
    }

    private void validerToken(ContainerRequestContext request, OpenIDProvider provider) {
        var tokenString = getTokenFreHeader(request);
        validerTokenSetKontekst(tokenString, provider);
        setUserAndConsumerId(KontekstHolder.getKontekst().getUid());
    }

    private static TokenString getTokenFreHeader(ContainerRequestContext request) {
        return getTokenFromHeader(request).orElseThrow(() -> new WebApplicationException("Mangler token", Response.Status.UNAUTHORIZED));
    }

    private static Optional<TokenString> getTokenFromHeader(ContainerRequestContext request) {
        String headerValue = request.getHeaderString(HttpHeaders.AUTHORIZATION);
        return headerValue != null && headerValue.startsWith(OpenIDToken.OIDC_DEFAULT_TOKEN_TYPE) ? Optional.of(new TokenString(headerValue.substring(OpenIDToken.OIDC_DEFAULT_TOKEN_TYPE.length()))) : Optional.empty();
    }

    private static void validerTokenSetKontekst(TokenString tokenString, OpenIDProvider forventetTokenProvider) {
        try {
            Objects.requireNonNull(forventetTokenProvider, "Forventet token validator mangler");
            LOG.debug("Forventet token type: {}", forventetTokenProvider);

            var claims = JwtUtil.getClaims(tokenString.token());
            var tokenIssuer = Optional.ofNullable(JwtUtil.getIssuer(claims)).orElseThrow(() -> new WebApplicationException("Token mangler issuer."));
            LOG.debug("Issuer fra token: {}", tokenIssuer);

            var openIdKonfig = ConfigProvider.getOpenIDConfiguration(tokenIssuer).orElseThrow(() -> new WebApplicationException("Issuer støttes ikke: " + tokenIssuer));

            var tokenProvider = openIdKonfig.type();
            if (!Objects.equals(tokenProvider, forventetTokenProvider)) {
                throw new WebApplicationException(String.format("Trenger en gyldig %s token, men har fått %s.", forventetTokenProvider, tokenProvider));
            }

            // Valider
            var tokenValidator = OidcTokenValidatorConfig.instance().getValidator(forventetTokenProvider);
            var validateResult = tokenValidator.validate(tokenString);

            // Håndter valideringsresultat
            if (validateResult.isValid()) {
                var expiresAt = Optional.ofNullable(JwtUtil.getExpirationTime(claims)).orElseThrow(() -> new WebApplicationException("Token mangler expires at claim"));
                var token = new OpenIDToken(tokenProvider, OpenIDToken.OIDC_DEFAULT_TOKEN_TYPE, tokenString, null, expiresAt.toEpochMilli());
                KontekstHolder.setKontekst(RequestKontekst.forRequest(validateResult.subject(), validateResult.compactSubject(), validateResult.identType(), token, validateResult.getGrupper()));
                LOG.trace("token validert");
            } else {
                throw new WebApplicationException("Ugyldig token");
            }
        } catch (TekniskException e) {
            throw new WebApplicationException(e, Response.Status.FORBIDDEN);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
        }
    }

    private ResourceInfo getResourceinfo() {
        return resourceinfo;
    }
}
