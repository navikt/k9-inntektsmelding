package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import static java.time.format.DateTimeFormatter.ofPattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OmsorgspengerPdfData {
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
        OmsorgspengerPdfData that = (OmsorgspengerPdfData) o;
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

    public static String formaterPersonnummer(String personnummer) {
        if (personnummer != null && personnummer.length() == 11) {
            var formatertPersonnummer = new StringBuilder(personnummer);
            formatertPersonnummer.insert(6, " ");
            return formatertPersonnummer.toString();
        }
        return personnummer;
    }

    public static String formaterDatoForLister(LocalDate dato) {
        if (dato == null) {
            return null;
        }
        return dato.format(ofPattern("dd.MM.yyyy", Locale.forLanguageTag("NO")));
    }

    public static String formaterDatoMedNavnPåUkedag(LocalDate dato) {
        if (dato == null) {
            return null;
        }
        var navnPåUkedag = dato.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("NO"));
        navnPåUkedag = navnPåUkedag.substring(0,1).toUpperCase() + navnPåUkedag.substring(1);
        return navnPåUkedag + " " + dato.format(ofPattern("d. MMMM yyyy", Locale.forLanguageTag("NO")));
    }

    public static String formaterDatoOgTidNorsk(LocalDateTime opprettetTidspunkt) {
        if (opprettetTidspunkt == null) {
            return null;
        }
        return opprettetTidspunkt.format(ofPattern("d. MMMM yyyy HH:mm:ss", Locale.forLanguageTag("NO")));
    }

    public void anonymiser() {
        this.personnummer = personnummer.substring(0, 4) + "** *****";
        this.arbeidsgiverIdent = arbeidsgiverIdent.substring(0, 4) + "** *****";
    }

    public static class Builder {
        private OmsorgspengerPdfData kladd;

        public Builder() {
            kladd = new OmsorgspengerPdfData();
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
            this.kladd.personnummer = formaterPersonnummer(personnummer);
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
            this.kladd.opprettetTidspunkt = formaterDatoOgTidNorsk(opprettetTidspunkt);
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

        public OmsorgspengerPdfData build() {
            return kladd;
        }
    }
}
