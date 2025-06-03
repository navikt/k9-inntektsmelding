package no.nav.familie.inntektsmelding.forespørsel.modell;

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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@SequenceGenerator(name = "GLOBAL_PK_SEQ_GENERATOR", sequenceName = "SEQ_GLOBAL_PK")
@Entity(name = "DelvisFraværsDagForespørselEntitet")
@Table(name = "DELVIS_FRAVAERS_DAG_FORESPOERSEL")
public class DelvisFraværsDagForespørselEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "OMSORGSPENGER_FORESPOERSEL_ID", nullable = false, updatable = false)
    private OmsorgspengerForespørselEntitet omsorgspengerForespørsel;

    @Column(name = "DATO", nullable = false, updatable = false)
    private LocalDate dato;

    @Column(name = "FRAVAERS_TIMER", nullable = false, updatable = false)
    private BigDecimal fraværstimer;

    @Column(name = "NORMAL_ARBEIDSTID", nullable = false, updatable = false)
    private BigDecimal normalArbeidstid;

    DelvisFraværsDagForespørselEntitet() {
        // Hibernate
    }

    public DelvisFraværsDagForespørselEntitet(LocalDate dato, BigDecimal fraværstimer, BigDecimal normalArbeidstid) {
        this.dato = dato;
        this.fraværstimer = fraværstimer;
        this.normalArbeidstid = normalArbeidstid;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getDato() {
        return dato;
    }

    public BigDecimal getFraværstimer() {
        return fraværstimer;
    }

    public BigDecimal getNormalArbeidstid() {
        return normalArbeidstid;
    }

    public OmsorgspengerForespørselEntitet getOmsorgspengerForespørsel() {
        return omsorgspengerForespørsel;
    }

    public void setOmsorgspengerForespørsel(OmsorgspengerForespørselEntitet omsorgspengerForespørsel) {
        this.omsorgspengerForespørsel = omsorgspengerForespørsel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DelvisFraværsDagForespørselEntitet that)) {
            return false;
        }

        return Objects.equals(dato, that.dato) &&
               Objects.equals(fraværstimer, that.fraværstimer) &&
               Objects.equals(normalArbeidstid, that.normalArbeidstid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dato, fraværstimer, normalArbeidstid);
    }

    @Override
    public String toString() {
        return "DelvisFraværsDagForespørselEntitet{" +
               "id=" + id +
               ", dato=" + dato +
               ", fraværstimer=" + fraværstimer +
               ", normalArbeidstid=" + normalArbeidstid +
               '}';
    }
}
