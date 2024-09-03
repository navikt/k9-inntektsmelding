package no.nav.familie.inntektsmelding.server.authz.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.enterprise.util.Nonbinding;
import jakarta.ws.rs.NameBinding;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@NameBinding
public @interface Tilgangsstyring {
    @Nonbinding PolicyType policy();

    @Nonbinding ActionType action();

    /**
     * Sett til false for å unngå at det logges til sporingslogg ved tilgang. Det
     * skal bare gjøres for tilfeller som ikke håndterer personopplysninger.
     */
    @Nonbinding boolean sporingslogg() default true;
}
