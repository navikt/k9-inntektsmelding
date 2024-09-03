package no.nav.familie.inntektsmelding.server.authz.api;

import static java.util.Objects.requireNonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TilgangsstyringInput {

    private final Map<TilgangsstyringInputType, Set<Object>> attributter = new LinkedHashMap<>();

    public static TilgangsstyringInput opprett() {
        return new TilgangsstyringInput();
    }

    public TilgangsstyringInput leggTil(TilgangsstyringInput annen) {
        for (var entry : annen.attributter.entrySet()) {
            if (entry.getValue() != null) {
                leggTil(entry.getKey(), entry.getValue());
            }
        }
        return this;
    }

    public Set<TilgangsstyringInputType> keySet() {
        return attributter.keySet();
    }

    public TilgangsstyringInput leggTil(TilgangsstyringInputType type, Collection<Object> samling) {
        var a = attributter.get(type);
        if (a == null) {
            attributter.put(type, new LinkedHashSet<>(samling));
        } else {
            a.addAll(samling);
        }
        return this;
    }

    public TilgangsstyringInput leggTil(TilgangsstyringInputType type, Object verdi) {
        requireNonNull(verdi, "Attributt av type " + type + " kan ikke være null");
        attributter.computeIfAbsent(type, k -> new LinkedHashSet<>(4))
            .add(verdi);
        return this;
    }

    public <T> Set<T> getVerdier(TilgangsstyringInputType type) {
        return attributter.containsKey(type) ? (Set<T>) attributter.get(type) : Collections.emptySet();
    }

    @Override
    public String toString() {
        return TilgangsstyringInput.class.getSimpleName() + "{" + attributter.entrySet()
            .stream()
            .map(e -> e.getKey() + "=" + (e.getKey().erMaskert() ? maskertEllerTom(e.getValue()) : e.getValue()))
            .collect(Collectors.joining(", ", "{", "}"));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TilgangsstyringInput annen)) {
            return false;
        }
        return Objects.equals(attributter, annen.attributter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributter);
    }

    private static String maskertEllerTom(Collection<?> input) {
        return input.isEmpty() ? "[]" : "[MASKERT#" + input.size() + "]";
    }
}
