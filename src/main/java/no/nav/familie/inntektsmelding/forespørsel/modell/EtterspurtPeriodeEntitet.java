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
@Entity(name = "EtterspurtPeriodeEntitet")
@Table(name = "ETTERSPURT_PERIODE")
public class EtterspurtPeriodeEntitet {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "FORESPOERSEL_ID", nullable = false, updatable = false)
    private ForespørselEntitet forespørsel;

    @Embedded
    private PeriodeEntitet periode;

    EtterspurtPeriodeEntitet() {
        // Hibernate
    }

    public EtterspurtPeriodeEntitet(PeriodeEntitet periode) {
        this.periode = periode;
    }

    public Long getId() {
        return id;
    }

    public PeriodeEntitet getPeriode() {
        return periode;
    }

    public ForespørselEntitet getForespørsel() {
        return forespørsel;
    }

    public void setForespørsel(ForespørselEntitet forespørsel) {
        this.forespørsel = forespørsel;
    }

    @Override
    public String toString() {
        return "EtterspurtPeriodeEntitet{" +
               "id=" + id +
               ", forespørsel=" + (forespørsel != null ? forespørsel.getUuid() : null) +
               ", periode=" + periode +
               '}';
    }
}
