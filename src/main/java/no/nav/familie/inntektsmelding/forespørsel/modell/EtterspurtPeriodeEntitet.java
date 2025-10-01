package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.time.LocalDate;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import no.nav.familie.inntektsmelding.imdialog.modell.PeriodeEntitet;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;

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

    private EtterspurtPeriodeEntitet() {
        // Hibernate
    }

    EtterspurtPeriodeEntitet(ForespørselEntitet forespørsel, PeriodeDto periode) {
        this.forespørsel = forespørsel;
        this.periode = PeriodeEntitet.fraOgMedTilOgMed(periode.fom(), periode.tom());
    }

    public Long getId() {
        return id;
    }

    public LocalDate getFom() {
        return periode.getFom();
    }

    public LocalDate getTom() {
        return periode.getTom();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EtterspurtPeriodeEntitet that)) {
            return false;
        }

        return periode.equals(that.periode);
    }
}
