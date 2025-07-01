package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import no.nav.familie.inntektsmelding.utils.FormatUtils;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OmsorgspengerRefusjonPdfData {
    private String avsenderSystem;
    private String navnSøker;
    private String personnummer;
    private String arbeidsgiverIdent;
    private String arbeidsgiverNavn;
    private Kontaktperson kontaktperson;
    private BigDecimal månedInntekt;
    private String opprettetTidspunkt;
    private List<Endringsarsak> endringsarsaker = new ArrayList<>();
    private Omsorgspenger omsorgspenger;
    private BigDecimal årForRefusjon;

    public String getAvsenderSystem() {
        return avsenderSystem;
    }

    public String getNavnSøker() {
        return navnSøker;
    }

    public String getPersonnummer() {
        return personnummer;
    }

    public String getArbeidsgiverIdent() {
        return arbeidsgiverIdent;
    }

    public String getArbeidsgiverNavn() {
        return arbeidsgiverNavn;
    }

    public Kontaktperson getKontaktperson() {
        return kontaktperson;
    }

    public BigDecimal getMånedInntekt() {
        return månedInntekt;
    }

    public String getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public List<Endringsarsak> getEndringsarsaker() {
        return endringsarsaker;
    }

    public Omsorgspenger getOmsorgspenger() {
        return omsorgspenger;
    }

    public BigDecimal getÅrForRefusjon() {
        return årForRefusjon;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OmsorgspengerRefusjonPdfData that = (OmsorgspengerRefusjonPdfData) o;
        return Objects.equals(avsenderSystem, that.avsenderSystem)
            && Objects.equals(navnSøker, that.navnSøker)
            && Objects.equals(personnummer, that.personnummer)
            && Objects.equals(arbeidsgiverIdent, that.arbeidsgiverIdent)
            && Objects.equals(arbeidsgiverNavn, that.arbeidsgiverNavn)
            && Objects.equals(kontaktperson, that.kontaktperson)
            && Objects.equals(månedInntekt, that.månedInntekt)
            && Objects.equals(opprettetTidspunkt, that.opprettetTidspunkt)
            && Objects.equals(endringsarsaker, that.endringsarsaker)
            && Objects.equals(omsorgspenger, that.omsorgspenger)
            && Objects.equals(årForRefusjon, that.årForRefusjon);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avsenderSystem, navnSøker, personnummer, arbeidsgiverIdent, arbeidsgiverNavn, kontaktperson, månedInntekt,
            opprettetTidspunkt, endringsarsaker,omsorgspenger, årForRefusjon);
    }

    public void anonymiser() {
        this.personnummer = personnummer.substring(0, 4) + "** *****";
        this.arbeidsgiverIdent = arbeidsgiverIdent.substring(0, 4) + "** *****";
    }

    public static class Builder {
        private OmsorgspengerRefusjonPdfData kladd;

        public Builder() {
            kladd = new OmsorgspengerRefusjonPdfData();
        }

        public Builder medAvsenderSystem(String avsenderSystem) {
            this.kladd.avsenderSystem = avsenderSystem;
            return this;
        }

        public Builder medNavn(String navn) {
            this.kladd.navnSøker = navn;
            return this;
        }

        public Builder medPersonnummer(String personnummer) {
            this.kladd.personnummer = FormatUtils.formaterPersonnummer(personnummer);
            return this;
        }

        public Builder medArbeidsgiverIdent(String arbeidsgiverIdent) {
            this.kladd.arbeidsgiverIdent = arbeidsgiverIdent;
            return this;
        }

        public Builder medArbeidsgiverNavn(String arbeidsgiverNavn) {
            this.kladd.arbeidsgiverNavn = arbeidsgiverNavn;
            return this;
        }

        public Builder medMånedInntekt(BigDecimal månedInntekt) {
            this.kladd.månedInntekt = månedInntekt;
            return this;
        }

        public Builder medOpprettetTidspunkt(LocalDateTime opprettetTidspunkt) {
            this.kladd.opprettetTidspunkt = FormatUtils.formaterDatoOgTidNorsk(opprettetTidspunkt);
            return this;
        }

        public Builder medKontaktperson(Kontaktperson kontaktperson) {
            this.kladd.kontaktperson = kontaktperson;
            return this;
        }

        public Builder medEndringsårsaker(List<Endringsarsak> endringsårsaker) {
            this.kladd.endringsarsaker = endringsårsaker;
            return this;
        }

        public Builder medOmsorgspenger(Omsorgspenger omsorgspenger) {
            this.kladd.omsorgspenger = omsorgspenger;
            return this;
        }

        public Builder medÅrForRefusjon(BigDecimal årForRefusjon) {
            this.kladd.årForRefusjon = årForRefusjon;
            return this;
        }

        public OmsorgspengerRefusjonPdfData build() {
            return kladd;
        }
    }
}
