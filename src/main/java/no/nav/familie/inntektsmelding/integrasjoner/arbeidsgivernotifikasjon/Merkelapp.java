package no.nav.familie.inntektsmelding.integrasjoner.arbeidsgivernotifikasjon;

public enum Merkelapp {
    INNTEKTSMELDING_PSB("Inntektsmelding pleiepenger sykt barn"),
    INNTEKTSMELDING_PILS("Inntektsmelding pleiepenger i livets sluttfase"),
    INNTEKTSMELDING_OPP("Inntektsmelding opplæringspenger"),
    INNTEKTSMELDING_OMP("Inntektsmelding omsorgspenger"),
    REFUSJONSKRAV_OMP("Refusjonskrav for omsorgspenger");

    private final String beskrivelse;

    Merkelapp(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }
}


