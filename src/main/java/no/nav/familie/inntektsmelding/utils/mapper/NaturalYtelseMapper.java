package no.nav.familie.inntektsmelding.utils.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.vedtak.konfig.Tid;

public class NaturalYtelseMapper {
    private static final Logger LOG = LoggerFactory.getLogger(NaturalYtelseMapper.class);

    private NaturalYtelseMapper() {
        // static
    }

    public static List<NaturalYtelse> mapNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalYtelseEntiteter) {

        var bortfalteNaturalYtelser = naturalYtelseEntiteter.stream()
            .map(NaturalYtelseMapper::mapBortfalteNaturalYtelser)
            .toList();

        List<NaturalYtelse> resultat = new ArrayList<>(bortfalteNaturalYtelser);
        LOG.info("Fant {} bortfalte naturalytelser", resultat.size());

        var tilkomneNaturalYtelser = bortfalteNaturalYtelser.stream()
            .filter(bn -> bn.tom() != null)
            .map(bortfalt -> mapTilkomneNaturalYtelser(bortfalt, bortfalteNaturalYtelser))
            .toList();

        LOG.info("Utledet {} tilkomne naturalytelser", tilkomneNaturalYtelser.size());

        resultat.addAll(tilkomneNaturalYtelser);
        return resultat;
    }

    private static NaturalYtelse mapBortfalteNaturalYtelser(BortaltNaturalytelseEntitet bortfalt) {
        return new NaturalYtelse(
            bortfalt.getPeriode().getFom(),
            bortfalt.getPeriode().getTom().equals(Tid.TIDENES_ENDE) ? null : bortfalt.getPeriode().getTom(),
            bortfalt.getType(),
            bortfalt.getMånedBeløp(),
            true);
    }

    private static NaturalYtelse mapTilkomneNaturalYtelser(NaturalYtelse tilkommet,
                                                           List<NaturalYtelse> alleBortfalteNarutalYtelser) {

        var tomForTilkommet = finnNesteTomForTilkommet(tilkommet, alleBortfalteNarutalYtelser).orElse(null);

        return new NaturalYtelse(tilkommet.tom().plusDays(1),
            tomForTilkommet,
            tilkommet.type(),
            tilkommet.beløp(),
            false);

    }

    private static Optional<LocalDate> finnNesteTomForTilkommet(NaturalYtelse tilkommet,
                                                                List<NaturalYtelse> bortfalteNaturalYtelser) {
        var nesteTom = bortfalteNaturalYtelser.stream()
            .filter(bortfalteYtelser -> bortfalteYtelser.type().equals(tilkommet.type()) && bortfalteYtelser.fom().isAfter(tilkommet.tom()))
            .map(NaturalYtelse::fom)
            .min(Comparator.naturalOrder());

        return nesteTom.map(d -> d.minusDays(1));
    }

    public record NaturalYtelse(LocalDate fom, LocalDate tom, NaturalytelseType type, BigDecimal beløp, boolean bortfallt) {
    }

}
