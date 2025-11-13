package no.nav.familie.inntektsmelding.integrasjoner.dokgen;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.utils.FormatUtils;

@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class InntektsmeldingPdfData {
    private String avsenderSystem;
    private String navnSøker;
    private String personnummer;
    private Ytelsetype ytelsetype;
    private String arbeidsgiverIdent;
    private String arbeidsgiverNavn;
    private Kontaktperson kontaktperson;
    private String startDato;
    private BigDecimal månedInntekt;
    private String opprettetTidspunkt;
    private List<RefusjonsendringPeriode> refusjonsendringer = new ArrayList<>();
    private List<NaturalYtelse> naturalytelser = new ArrayList<>();
    private boolean ingenBortfaltNaturalytelse;
    private boolean ingenGjenopptattNaturalytelse;
    private List<Endringsarsak> endringsarsaker = new ArrayList<>();
    private int antallRefusjonsperioder;

    public String getAvsenderSystem() {
        return avsenderSystem;
    }

    public String getNavnSøker() {
        return navnSøker;
    }

    public String getPersonnummer() {
        return personnummer;
    }

    public Ytelsetype getYtelsetype() {
        return ytelsetype;
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

    public String getStartDato() {
        return startDato;
    }

    public BigDecimal getMånedInntekt() {
        return månedInntekt;
    }

    public String getOpprettetTidspunkt() {
        return opprettetTidspunkt;
    }

    public List<RefusjonsendringPeriode> getRefusjonsendringer() {
        return refusjonsendringer;
    }

    public List<NaturalYtelse> getNaturalytelser() {
        return naturalytelser;
    }

    public boolean ingenGjenopptattNaturalytelse() {
        return ingenGjenopptattNaturalytelse;
    }

    public boolean ingenBortfaltNaturalytelse() {
        return ingenBortfaltNaturalytelse;
    }

    public List<Endringsarsak> getEndringsarsaker() {
        return endringsarsaker;
    }

    public int getAntallRefusjonsperioder() {
        return antallRefusjonsperioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        InntektsmeldingPdfData that = (InntektsmeldingPdfData) o;
        return ingenBortfaltNaturalytelse == that.ingenBortfaltNaturalytelse
            && ingenGjenopptattNaturalytelse == that.ingenGjenopptattNaturalytelse
            && Objects.equals(avsenderSystem, that.avsenderSystem)
            && Objects.equals(navnSøker, that.navnSøker)
            && Objects.equals(personnummer, that.personnummer)
            && ytelsetype == that.ytelsetype
            && Objects.equals(arbeidsgiverIdent, that.arbeidsgiverIdent)
            && Objects.equals(arbeidsgiverNavn, that.arbeidsgiverNavn)
            && Objects.equals(kontaktperson, that.kontaktperson)
            && Objects.equals(startDato, that.startDato)
            && Objects.equals(månedInntekt, that.månedInntekt)
            && Objects.equals(opprettetTidspunkt, that.opprettetTidspunkt)
            && Objects.equals(refusjonsendringer, that.refusjonsendringer)
            && Objects.equals(naturalytelser, that.naturalytelser)
            && Objects.equals(endringsarsaker, that.endringsarsaker)
            && Objects.equals(antallRefusjonsperioder, that.antallRefusjonsperioder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(avsenderSystem, navnSøker, personnummer, ytelsetype, arbeidsgiverIdent, arbeidsgiverNavn, kontaktperson,
            startDato, månedInntekt, opprettetTidspunkt, refusjonsendringer, naturalytelser,
            ingenBortfaltNaturalytelse, ingenGjenopptattNaturalytelse, endringsarsaker, antallRefusjonsperioder);
    }

    public void anonymiser() {
        this.personnummer = personnummer.substring(0, 4) + "** *****";
        this.arbeidsgiverIdent = arbeidsgiverIdent.substring(0, 4) + "** *****";
    }

    public static class Builder {
        private InntektsmeldingPdfData kladd;

        public Builder() {
            kladd = new InntektsmeldingPdfData();
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

        public Builder medYtelseNavn(Ytelsetype ytelsenavn) {
            this.kladd.ytelsetype = ytelsenavn;
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

        public Builder medStartDato(LocalDate startDato) {
            this.kladd.startDato = FormatUtils.formaterDatoMedNavnPåUkedag(startDato);
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

        public Builder medRefusjonsendringer(List<RefusjonsendringPeriode> refusjonsperioder) {
            this.kladd.refusjonsendringer = refusjonsperioder;
            return this;
        }

        public Builder medNaturalytelser(List<NaturalYtelse> naturalYtelser) {
            this.kladd.naturalytelser = naturalYtelser;
            return this;
        }

        public Builder medIngenBortfaltNaturalytelse(boolean ingenBortfalt) {
            this.kladd.ingenBortfaltNaturalytelse = ingenBortfalt;
            return this;
        }

        public Builder medIngenGjenopptattNaturalytelse(boolean ingenGjennopptatt) {
            this.kladd.ingenGjenopptattNaturalytelse = ingenGjennopptatt;
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

        public Builder medAntallRefusjonsperioder(int antallRefusjonsperioder) {
            this.kladd.antallRefusjonsperioder = antallRefusjonsperioder;
            return this;
        }

        public InntektsmeldingPdfData build() {
            return kladd;
        }
    }
}
