package no.nav.familie.inntektsmelding.integrasjoner.dokgen;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import no.nav.familie.inntektsmelding.utils.FormatUtils;

// TODO: rydd opp i denne filen. Fjern ting vi ikke trenger og vurder å bruke metoder for bedre formatiering av datoer og personnummer. Lag også tester
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OmsorgspengerInntektsmeldingPdfData {
    private String avsenderSystem;
    private String navnSøker;
    private String personnummer;
    private String arbeidsgiverIdent;
    private String arbeidsgiverNavn;
    private Kontaktperson kontaktperson;
    private BigDecimal månedInntekt;
    private String opprettetTidspunkt;
    private List<Endringsarsak> endringsarsaker = new ArrayList<>();
    private FraværsInfo fraværsInfo;

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

    public FraværsInfo getFraværsInfo() {
        return fraværsInfo;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OmsorgspengerInntektsmeldingPdfData that = (OmsorgspengerInntektsmeldingPdfData) o;
        return Objects.equals(avsenderSystem, that.avsenderSystem)
            && Objects.equals(navnSøker, that.navnSøker)
            && Objects.equals(personnummer, that.personnummer)
            && Objects.equals(arbeidsgiverIdent, that.arbeidsgiverIdent)
            && Objects.equals(arbeidsgiverNavn, that.arbeidsgiverNavn)
            && Objects.equals(kontaktperson, that.kontaktperson)
            && Objects.equals(månedInntekt, that.månedInntekt)
            && Objects.equals(opprettetTidspunkt, that.opprettetTidspunkt)
            && Objects.equals(endringsarsaker, that.endringsarsaker)
            && Objects.equals(fraværsInfo, that.fraværsInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avsenderSystem, navnSøker, personnummer, arbeidsgiverIdent, arbeidsgiverNavn, kontaktperson, månedInntekt,
            opprettetTidspunkt, endringsarsaker,fraværsInfo);
    }

    public void anonymiser() {
        this.personnummer = personnummer.substring(0, 4) + "** *****";
        this.arbeidsgiverIdent = arbeidsgiverIdent.substring(0, 4) + "** *****";
    }

    public static class Builder {
        private OmsorgspengerInntektsmeldingPdfData kladd;

        public Builder() {
            kladd = new OmsorgspengerInntektsmeldingPdfData();
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

        public Builder medFraværsInfo(FraværsInfo fraværsInfo) {
            this.kladd.fraværsInfo = fraværsInfo;
            return this;
        }

        public OmsorgspengerInntektsmeldingPdfData build() {
            return kladd;
        }
    }
}
