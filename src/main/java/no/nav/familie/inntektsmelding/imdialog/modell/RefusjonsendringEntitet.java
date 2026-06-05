package no.nav.familie.inntektsmelding.imdialog.modell;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

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
public class RefusjonsendringEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "inntektsmelding_id", nullable = false, updatable = false)
    private InntektsmeldingEntitet inntektsmelding;

    @Column(name = "fom")
    private LocalDate fom;

    @Column(name = "maaned_refusjon")
    private BigDecimal refusjonPrMnd;

    RefusjonsendringEntitet() {
        // Hibernate
    }

    public RefusjonsendringEntitet(LocalDate fom, BigDecimal refusjonPrMnd) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RefusjonsendringEntitet that)) return false;
        return Objects.equals(fom, that.fom)
            && (refusjonPrMnd == null ? that.refusjonPrMnd == null : refusjonPrMnd.compareTo(that.refusjonPrMnd) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fom, refusjonPrMnd == null ? null : refusjonPrMnd.stripTrailingZeros());
    }

}
