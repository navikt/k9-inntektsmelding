package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Entity(name = "DelvisFraværsPeriodeEntitet")
@Table(name = "DELVIS_FRAVAERS_PERIODE")
public class DelvisFraværsPeriodeEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "omsorgspenger_id", nullable = false, updatable = false)
    private OmsorgspengerEntitet omsorgspenger;

    @Column(name = "dato", nullable = false)
    private LocalDate dato;

    @Column(name = "timer", nullable = false)
    private BigDecimal timer;

    DelvisFraværsPeriodeEntitet() {
        // Hibernate
    }

    public DelvisFraværsPeriodeEntitet(LocalDate dato, BigDecimal timer) {
        this.dato = dato;
        this.timer = timer;
    }

    public LocalDate getDato() {
        return dato;
    }

    public BigDecimal getTimer() {
        return timer;
    }

    void setOmsorgspenger(OmsorgspengerEntitet omsorgspenger) {
        this.omsorgspenger = omsorgspenger;
    }

    @Override
    public String toString() {
        return "DelvisFraværsPeriodeEntitet{" +
            "dato=" + dato +
            ", timer=" + timer +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DelvisFraværsPeriodeEntitet that)) return false;
        return Objects.equals(dato, that.dato)
            && (timer == null ? that.timer == null : timer.compareTo(that.timer) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dato, timer == null ? null : timer.stripTrailingZeros());
    }

}
