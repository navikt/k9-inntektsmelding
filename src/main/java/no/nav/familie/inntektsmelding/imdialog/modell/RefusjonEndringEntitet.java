package no.nav.familie.inntektsmelding.imdialog.modell;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "RefusjonEndringEntitet")
@Table(name = "REFUSJON_ENDRING")
public class RefusjonEndringEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_REFUSJON_ENDRING")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Column(name = "fom")
    private LocalDate fom;

    @Column(name = "maaned_refusjon")
    private BigDecimal refusjonPrMnd;

    public RefusjonEndringEntitet() {
        // Hibernate
    }

    public RefusjonEndringEntitet(LocalDate fom, BigDecimal refusjonPrMnd) {
        this.inntektsmelding = inntektsmelding;
        this.fom = fom;
        this.refusjonPrMnd = refusjonPrMnd;
    }

    public LocalDate getFom() {
        return fom;
    }

    public BigDecimal getRefusjonPrMnd() {
        return refusjonPrMnd;
    }

    void setInntektsmelding(InntektsmeldingEntitet inntektsmeldingEntitet) {
        this.inntektsmelding = inntektsmeldingEntitet;
    }

    @Override
    public String toString() {
        return "RefusjonEndringEntitet{" +
            "fom=" + fom +
            ", refusjonPrMnd=" + refusjonPrMnd +
            '}';
    }

}
