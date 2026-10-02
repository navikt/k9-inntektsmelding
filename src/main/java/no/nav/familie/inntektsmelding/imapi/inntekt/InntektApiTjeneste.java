package no.nav.familie.inntektsmelding.imapi.inntekt;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.Inntektsopplysninger;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.k9.inntektsmelding.imapi.inntekt.InntektResponse;


@ApplicationScoped
public class InntektApiTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektApiTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private PersonTjeneste personTjeneste;
    private InntektTjeneste inntektTjeneste;

    InntektApiTjeneste() {
        // CDI
    }

    @Inject
    public InntektApiTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                              PersonTjeneste personTjeneste,
                              InntektTjeneste inntektTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.personTjeneste = personTjeneste;
        this.inntektTjeneste = inntektTjeneste;
    }

    public Optional<InntektResponse> hentInntektDto(UUID forespørselUuid) {
        ForespørselEntitet forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid).orElse(null);
        if (forespørsel == null) {
            LOG.info("Forespørsel med uuid {} finnes ikke, returnerer ikke funnet", forespørselUuid);
            return Optional.empty();
        }

        PersonInfo personinfo = personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId());
        Inntektsopplysninger inntektsopplysninger = inntektTjeneste.hentInntekt(personinfo,
            forespørsel.getSkjæringstidspunkt(),
            LocalDate.now(),
            forespørsel.getOrganisasjonsnummer(),
            forespørsel.getYtelseType());

        if (inntektsopplysninger.harNedetid()) {
            LOG.info("Inntekt er ikke rapportert (nedetid i inntektskomponenten) for forespørsel med uuid {}, returnerer ikke funnet", forespørselUuid);
            return Optional.empty();
        }
        return Optional.of(mapTilDto(inntektsopplysninger));
    }

    private InntektResponse mapTilDto(Inntektsopplysninger inntektsopplysninger) {
        // Bruker LinkedHashMap manuelt (ikke Collectors.toMap) siden en måned kan mangle rapportert beløp (null),
        // og Map.merge (som Collectors.toMap benytter internt) ikke tillater null-verdier.
        Map<YearMonth, BigDecimal> inntektPerMåned = new LinkedHashMap<>();
        inntektsopplysninger.måneder().forEach(måned -> inntektPerMåned.put(måned.månedÅr(), måned.beløp()));
        return new InntektResponse(inntektPerMåned, inntektsopplysninger.gjennomsnitt());
    }
}
