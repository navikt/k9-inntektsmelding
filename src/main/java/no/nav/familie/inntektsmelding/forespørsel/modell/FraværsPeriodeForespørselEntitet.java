package no.nav.familie.inntektsmelding.forespørsel.modell;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;

@SequenceGenerator(name = "GLOBAL_PK_SEQ_GENERATOR", sequenceName = "SEQ_GLOBAL_PK")
@Entity(name = "FraværsPeriodeForespørselEntitet")
@Table(name = "FRAVAERS_PERIODE_FORESPOERSEL")
public class FraværsPeriodeForespørselEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "OMSORGSPENGER_FORESPOERSEL_ID", nullable = false, updatable = false)
    private OmsorgspengerForespørselEntitet omsorgspengerForespørsel;

    @Embedded
    private PeriodeEntitet periode;

    FraværsPeriodeForespørselEntitet() {
        // Hibernate
    }

    public FraværsPeriodeForespørselEntitet(PeriodeEntitet periode) {
        this.periode = periode;
    }

    public Long getId() {
        return id;
    }

    public PeriodeEntitet getPeriode() {
        return periode;
    }

    public OmsorgspengerForespørselEntitet getOmsorgspengerForespørsel() {
        return omsorgspengerForespørsel;
    }

    public void setOmsorgspengerForespørsel(OmsorgspengerForespørselEntitet omsorgspengerForespørsel) {
        this.omsorgspengerForespørsel = omsorgspengerForespørsel;
    }
}
