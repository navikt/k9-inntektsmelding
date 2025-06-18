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

}
