package no.nav.familie.inntektsmelding.imdialog.modell;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity(name = "FraværsPeriodeEntitet")
@Table(name = "FRAVAERS_PERIODE")
public class FraværsPeriodeEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "omsorgspenger_id", nullable = false, updatable = false)
    private OmsorgspengerEntitet omsorgspenger;

    @Embedded
    private PeriodeEntitet periode;

    public FraværsPeriodeEntitet() {
        // Hibernate
    }

    public FraværsPeriodeEntitet(PeriodeEntitet periode) {
        this.periode = periode;
    }

    public PeriodeEntitet getPeriode() {
        return periode;
    }

    public OmsorgspengerEntitet getOmsorgspenger() {
        return omsorgspenger;
    }

    void setOmsorgspenger(OmsorgspengerEntitet omsorgspenger) {
        this.omsorgspenger = omsorgspenger;
    }

    @Override
    public String toString() {
        return "FraværsPeriodeEntitet{" +
            "periode=" + periode +
            '}';
    }

}
