package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity(name = "NaturalytelseEntitet")
@Table(name = "NATURALYTELSE")
public class NaturalytelseEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_NATURALYTELSE")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Embedded
    private PeriodeEntitet periode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NaturalytelseType type;

    @Column(name = "beloep", nullable = false)
    private BigDecimal beløp;

    @Column(name = "er_bortfalt", nullable = false)
    private Boolean erBortfalt;

    public NaturalytelseEntitet() {
        // Hibernate
    }

    public PeriodeEntitet getPeriode() {
        return periode;
    }

    public NaturalytelseType getType() {
        return type;
    }

    public BigDecimal getBeløp() {
        return beløp;
    }

    public Boolean getErBortfalt() {
        return erBortfalt;
    }

    void setInntektsmelding(InntektsmeldingEntitet inntektsmelding) {
        this.inntektsmelding = inntektsmelding;
    }

    @Override
    public String toString() {
        return "NaturalytelseEntitet{" + "periode=" + periode + ", type=" + type + ", beløp=" + beløp + ", erBortfalt=" + erBortfalt + '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private NaturalytelseEntitet kladd = new NaturalytelseEntitet();

        public Builder medPeriode(LocalDate fom, LocalDate tom) {
            kladd.periode = tom == null
                ? PeriodeEntitet.fraOgMed(fom)
                : PeriodeEntitet.fraOgMedTilOgMed(fom, tom);
            return this;
        }

        public Builder medBeløp(BigDecimal beløp) {
            kladd.beløp = beløp;
            return this;
        }

        public Builder medType(NaturalytelseType type) {
            kladd.type = type;
            return this;
        }

        public Builder medErBortfalt(Boolean erBortfalt) {
            kladd.erBortfalt = erBortfalt;
            return this;
        }

        public NaturalytelseEntitet build() {
            return kladd;
        }
    }
}
