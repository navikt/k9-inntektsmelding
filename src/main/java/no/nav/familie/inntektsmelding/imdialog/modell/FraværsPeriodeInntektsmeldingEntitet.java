package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "FraværsPeriodeInntektsmeldingEntitet")
@Table(name = "FRAVAERS_PERIODE_INNTEKTSMELDING")
public class FraværsPeriodeInntektsmeldingEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "omsorgspenger_id", nullable = false, updatable = false)
    private OmsorgspengerInntektsmeldingEntitet omsorgspenger;

    @Embedded
    private PeriodeEntitet periode;

    public FraværsPeriodeInntektsmeldingEntitet() {
        // Hibernate
    }

    public FraværsPeriodeInntektsmeldingEntitet(PeriodeEntitet periode) {
        this.periode = periode;
    }

    public PeriodeEntitet getPeriode() {
        return periode;
    }

    public OmsorgspengerInntektsmeldingEntitet getOmsorgspenger() {
        return omsorgspenger;
    }

    void setOmsorgspenger(OmsorgspengerInntektsmeldingEntitet omsorgspenger) {
        this.omsorgspenger = omsorgspenger;
    }

    @Override
    public String toString() {
        return "FraværsPeriodeInntektsmeldingEntitet{" +
            "periode=" + periode +
            '}';
    }

}
