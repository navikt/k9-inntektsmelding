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

@Entity(name = "DelvisFraværsDagInntektsmeldingEntitet")
@Table(name = "DELVIS_FRAVAERS_DAG_INNTEKTSMELDING")
public class DelvisFraværsDagInntektsmeldingEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "omsorgspenger_id", nullable = false, updatable = false)
    private OmsorgspengerInntektsmeldingEntitet omsorgspenger;

    @Column(name = "dato", nullable = false)
    private LocalDate dato;

    @Column(name = "timer", nullable = false)
    private BigDecimal timer;

    public DelvisFraværsDagInntektsmeldingEntitet() {
        // Hibernate
    }

    public DelvisFraværsDagInntektsmeldingEntitet(LocalDate dato, BigDecimal timer) {
        this.dato = dato;
        this.timer = timer;
    }

    public LocalDate getDato() {
        return dato;
    }

    public BigDecimal getTimer() {
        return timer;
    }

    void setOmsorgspenger(OmsorgspengerInntektsmeldingEntitet omsorgspenger) {
        this.omsorgspenger = omsorgspenger;
    }

    @Override
    public String toString() {
        return "DelvisFraværsDagInntektsmeldingEntitet{" +
            "dato=" + dato +
            ", timer=" + timer +
            '}';
    }

}
