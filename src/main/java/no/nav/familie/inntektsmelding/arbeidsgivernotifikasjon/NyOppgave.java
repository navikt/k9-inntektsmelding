package no.nav.familie.inntektsmelding.arbeidsgivernotifikasjon;

import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.familie.inntektsmelding.integrasjon.arbeidsgivernotifikasjon.NyOppgaveResultat;

class NyOppgave implements NyOppgaveResultat {

    public static final String VELLYKET_TYPENAME = "NyOppgaveVellyket";

    @JsonProperty("__typename")
    private String typename;
    private String feilmelding;
    private String id;

    public NyOppgave() {
    }

    public String getTypename() {
        return typename;
    }

    public void setTypename(String typename) {
        this.typename = typename;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public void setFeilmelding(String feilmelding) {
        this.feilmelding = feilmelding;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
