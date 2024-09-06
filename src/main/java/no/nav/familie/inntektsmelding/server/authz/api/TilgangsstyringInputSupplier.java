package no.nav.familie.inntektsmelding.server.authz.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface TilgangsstyringInputSupplier {
    Class<? extends Function<Object, TilgangsstyringInput>> value();
}
