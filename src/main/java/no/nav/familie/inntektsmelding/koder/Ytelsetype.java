package no.nav.familie.inntektsmelding.koder;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Ytelsetype {
    FORELDREPENGER("FP"),
    SVANGERSKAPSPENGER("SVP"),
    PLEIEPENGER_SYKT_BARN("PSB"),
    PLEIEPENGER_I_LIVETS_SLUTTFASE("PPN"),
    OPPLÆRINGSPENGER("OLP"),
    OMSORGSPENGER("OMP");


    private final String kode;

    private static final Map<String, Ytelsetype> KODER = new LinkedHashMap<>();

    static {
        for (var v : values()) {
            if (KODER.putIfAbsent(v.kode, v) != null) {
                throw new IllegalArgumentException("Duplikat : " + v.kode);
            }
        }
    }

    Ytelsetype(String kode) {
        this.kode = kode;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Ytelsetype fraKode(String kode) {
        if (kode == null) {
            return null;
        }
        var ad = KODER.get(kode);
        if (ad == null) {
            throw new IllegalArgumentException("Ukjent Ytelsetype: for input " + kode);
        }
        return ad;
    }

    public String getKode() {
        return kode;
    }
}
