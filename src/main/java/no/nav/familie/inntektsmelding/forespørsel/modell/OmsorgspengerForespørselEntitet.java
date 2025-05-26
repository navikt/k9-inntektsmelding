package no.nav.familie.inntektsmelding.forespørsel.modell;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@SequenceGenerator(name = "GLOBAL_PK_SEQ_GENERATOR", sequenceName = "SEQ_GLOBAL_PK")
@Entity(name = "OmsorgspengerForespørselEntitet")
@Table(name = "OMSORGSPENGER_FORESPOERSEL")
public class OmsorgspengerForespørselEntitet {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "GLOBAL_PK_SEQ_GENERATOR")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "FORESPOERSEL_ID", nullable = false, updatable = false)
    private ForespørselEntitet forespoersel;

    @Column(name = "BEGRUNNELSE_FOR_SOEKNAD", nullable = false)
    private String begrunnelseForSøknad;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "omsorgspengerForespørsel")
    private List<FraværsPeriodeForespørselEntitet> fraværsPerioder = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "omsorgspengerForespørsel")
    private List<DelvisFraværsDagForespørselEntitet> delvisFraværsDager = new ArrayList<>();

    public OmsorgspengerForespørselEntitet() {
        // Hibernate
    }

    public OmsorgspengerForespørselEntitet(String begrunnelseForSøknad) {
        this.begrunnelseForSøknad = begrunnelseForSøknad;
    }

    public Long getId() {
        return id;
    }

    public String getBegrunnelseForSøknad() {
        return begrunnelseForSøknad;
    }

    public void leggTilFraværsPeriode(FraværsPeriodeForespørselEntitet fraværsPeriode) {
        fraværsPeriode.setOmsorgspengerForespørsel(this);
        fraværsPerioder.add(fraværsPeriode);
    }

    public void leggTilDelvisFraværsDag(DelvisFraværsDagForespørselEntitet delvisFraværsDag) {
        delvisFraværsDag.setOmsorgspengerForespørsel(this);
        delvisFraværsDager.add(delvisFraværsDag);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final OmsorgspengerForespørselEntitet kladd = new OmsorgspengerForespørselEntitet();

        public Builder medBegrunnelseForSøknad(String begrunnelseForSøknad) {
            kladd.begrunnelseForSøknad = begrunnelseForSøknad;
            return this;
        }

        public Builder medFraværsPerioder(List<FraværsPeriodeForespørselEntitet> fraværsPerioder) {
            if (fraværsPerioder != null) {
                fraværsPerioder.forEach(kladd::leggTilFraværsPeriode);
            }
            return this;
        }

        public Builder medDelvisFraværsDager(List<DelvisFraværsDagForespørselEntitet> delvisFraværsDager) {
            if (delvisFraværsDager != null) {
                delvisFraværsDager.forEach(kladd::leggTilDelvisFraværsDag);
            }
            return this;
        }

        public OmsorgspengerForespørselEntitet build() {
            return kladd;
        }
    }
}
