package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import no.nav.familie.inntektsmelding.koder.Naturalytelsetype;
import no.nav.familie.inntektsmelding.typer.entitet.PeriodeEntitet;

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

    @Column(name = "type", nullable = false)
    private Naturalytelsetype type;

    @Column(name = "beloep", nullable = false)
    private BigDecimal beløp;

    @Column(name = "erBortfalt", nullable = false)
    private Boolean erBortfalt;

    public NaturalytelseEntitet() {
        // Hibernate
    }

    public static class Builder {
        private NaturalytelseEntitet kladd = new NaturalytelseEntitet();

        public Builder() {

        }

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

        public Builder medType(Naturalytelsetype type) {
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
