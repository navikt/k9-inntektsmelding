package no.nav.familie.inntektsmelding.server.tilgangsstyring;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidsforholdDto;
import no.nav.familie.inntektsmelding.integrasjoner.aareg.ArbeidstakerTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.altinn.AltinnTilgangTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.pip.PipTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.sif.abac.kontrakt.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.exception.FunksjonellException;
import no.nav.vedtak.exception.ManglerTilgangException;
import no.nav.vedtak.sikkerhet.kontekst.AnsattGruppe;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.Kontekst;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;
import no.nav.vedtak.sikkerhet.kontekst.RequestKontekst;

@Dependent
public class TilgangTjeneste implements Tilgang {

    private static final Logger LOG = LoggerFactory.getLogger(TilgangTjeneste.class);

    private final AltinnTilgangTjeneste altinnTilgangTjeneste;
    private final PipTjeneste pipTjeneste;
    private final SifAbacPdpKlient sifAbacPdpKlient;
    private final ArbeidstakerTjeneste arbeidstakerTjeneste;
    private final InntektTjeneste inntektTjeneste;
    private final PersonTjeneste personTjeneste;

    @Inject
    public TilgangTjeneste(PipTjeneste pipTjeneste,
                           AltinnTilgangTjeneste altinnTilgangTjeneste,
                           SifAbacPdpKlient sifAbacPdpKlient,
                           ArbeidstakerTjeneste arbeidstakerTjeneste,
                           InntektTjeneste inntektTjeneste,
                           PersonTjeneste personTjeneste) {
        this.pipTjeneste = pipTjeneste;
        this.altinnTilgangTjeneste = altinnTilgangTjeneste;
        this.sifAbacPdpKlient = sifAbacPdpKlient;
        this.arbeidstakerTjeneste = arbeidstakerTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.personTjeneste = personTjeneste;
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilBedrift(UUID forespørselUuid) {
        sjekkErBorger();

        var orgNrSet = Optional.ofNullable(forespørselUuid)
            .stream()
            .map(pipTjeneste::hentOrganisasjonsnummerFor)
            .filter(Objects::nonNull)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        sjekkBorgersAltinnTilgangTilOrganisasjon(orgNrSet);
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilBedrift(OrganisasjonsnummerDto organisasjonsnummer) {
        sjekkErBorger();

        sjekkBorgersAltinnTilgangTilOrganisasjon(Set.of(organisasjonsnummer.orgnr()));
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilBedrift(long inntektsmeldingId) {
        sjekkErBorger();

        var orgNrSet = Optional.of(inntektsmeldingId)
            .stream()
            .map(pipTjeneste::hentOrganisasjonsnummerFor)
            .filter(Objects::nonNull)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        sjekkBorgersAltinnTilgangTilOrganisasjon(orgNrSet);
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilInntektsmelding(UUID inntektsmeldingUUID) {
        sjekkErBorger();

        var orgNrSet = Optional.ofNullable(inntektsmeldingUUID)
            .stream()
            .map(pipTjeneste::hentInntektsmeldingOrganisasjonsnummer)
            .filter(Objects::nonNull)
            .map(OrganisasjonsnummerDto::orgnr)
            .collect(Collectors.toSet());

        sjekkBorgersAltinnTilgangTilOrganisasjon(orgNrSet);
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilForespørsel(UUID forespørselUuid, AktørIdEntitet aktørId, OrganisasjonsnummerDto organisasjonsnummer) {
        sjekkErBorger();

        var forespørsel = Optional.ofNullable(forespørselUuid).flatMap(pipTjeneste::hentForespørselTilgangInfo);
        if (forespørsel.isEmpty()) {
            ikkeTilgang("Mangler informasjon om bedrift.");
            return;
        }
        if (!forespørsel.get().organisasjonsnummer().equals(organisasjonsnummer)) {
            ikkeTilgang("Organisasjonsnummer stemmer ikke med forespørselen.");
        }
        if (!forespørsel.get().aktørId().equals(aktørId)) {
            ikkeTilgang("Arbeidstaker stemmer ikke med forespørselen.");
        }

        sjekkBorgersAltinnTilgangTilOrganisasjon(Set.of(forespørsel.get().organisasjonsnummer().orgnr()));
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilArbeidstaker(PersonIdent fødselsnummer, OrganisasjonsnummerDto organisasjonsnummer, LocalDate dato) {
        sjekkErBorger();
        sjekkBorgersAltinnTilgangTilOrganisasjon(Set.of(organisasjonsnummer.orgnr()));
        sjekkArbeidsforholdIAareg(fødselsnummer, organisasjonsnummer, dato);
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilArbeidstaker(AktørIdEntitet aktørId, OrganisasjonsnummerDto organisasjonsnummer, LocalDate dato) {
        sjekkErBorger();
        sjekkBorgersAltinnTilgangTilOrganisasjon(Set.of(organisasjonsnummer.orgnr()));
        sjekkArbeidsforholdIAareg(personTjeneste.finnPersonIdentForAktørId(aktørId), organisasjonsnummer, dato);
    }

    @Override
    public void sjekkAtArbeidsgiverHarTilgangTilArbeidstakerMedInntekt(AktørIdEntitet aktørId,
                                                                       OrganisasjonsnummerDto organisasjonsnummer,
                                                                       LocalDate dato,
                                                                       Ytelsetype ytelsetype) {
        sjekkErBorger();
        sjekkBorgersAltinnTilgangTilOrganisasjon(Set.of(organisasjonsnummer.orgnr()));

        var personInfo = personTjeneste.hentPersonInfoFraAktørId(aktørId);
        var inntekt = inntektTjeneste.hentInntekt(personInfo, dato, LocalDate.now(), organisasjonsnummer.orgnr(), ytelsetype);
        if (inntekt.harNedetid()) {
            throw new FunksjonellException("A_INNTEKT_NEDE", "Kunne ikke hente inntekt fra a-inntekt. Prøv igjen senere", null, null);
        }
        if (inntekt.gjennomsnitt() == null || inntekt.gjennomsnitt().compareTo(BigDecimal.ZERO) <= 0) {
            ikkeTilgang("Arbeidstaker har ikke inntekt i bedriften.");
        }
    }

    private void sjekkArbeidsforholdIAareg(PersonIdent fødselsnummer, OrganisasjonsnummerDto organisasjonsnummer, LocalDate dato) {
        var harArbeidsforholdIBedriften = arbeidstakerTjeneste.finnArbeidsforholdInnsenderHarTilgangTil(fødselsnummer, dato, dato)
            .stream()
            .filter(arbeidsforhold -> organisasjonsnummer.orgnr().equals(arbeidsforhold.organisasjonsnummer()))
            .anyMatch(arbeidsforhold -> erAktivPåDato(arbeidsforhold.ansettelsesperiode(), dato));
        if (!harArbeidsforholdIBedriften) {
            ikkeTilgang("Arbeidstaker har ikke arbeidsforhold i bedriften.");
        }
    }

    private static boolean erAktivPåDato(ArbeidsforholdDto.Ansettelsesperiode periode, LocalDate dato) {
        if (periode == null || periode.fom() == null) {
            return false;
        }
        return !dato.isBefore(periode.fom()) && (periode.tom() == null || !dato.isAfter(periode.tom()));
    }

    @Override
    public void sjekkAtAnsattHarRollenDrift() {
        var kontekst = KontekstHolder.getKontekst();
        if (erNavAnsatt(kontekst) && ansattHarRollen(kontekst, AnsattGruppe.DRIFT)) {
            return;
        }
        ikkeTilgang("Ansatt mangler en rolle.");
    }

    @Override
    public void sjekkAtAnsattHarTilgangTilSak(String saksnummer, BeskyttetRessursActionAttributt aksjon) {
        var tilgang = sifAbacPdpKlient.harAnsattTilgangTilSak(saksnummer, aksjon);
        if (tilgang.isPresent()) {
            if (tilgang.get().tilgangsbeslutning().harTilgang()) {
                return;
            }
            ikkeTilgang(SifAbacPdpUtil.hentBegrunnelse(tilgang.get().tilgangsbeslutning().årsakerForIkkeTilgang()));
        }
        ikkeTilgang("Ansatt mangler en rolle.");
    }

    @Override
    public void sjekkErSystembruker() {
        if (KontekstHolder.getKontekst() instanceof RequestKontekst rq && rq.getIdentType().erSystem()) {
            return;
        }
        ikkeTilgang("Kun systemkall støttes.");
    }

    @Override
    public void sjekkErSystembrukerEllerAtSaksbehandlerHarTilgangTilSak(String saksnummer, BeskyttetRessursActionAttributt aksjon) {
        if (KontekstHolder.getKontekst() instanceof RequestKontekst rq && rq.getIdentType().erSystem()) {
            return;
        }
        var tilgang = sifAbacPdpKlient.harAnsattTilgangTilSak(saksnummer, aksjon);
        if (tilgang.isPresent()) {
            if (tilgang.get().tilgangsbeslutning().harTilgang()) {
                return;
            }
            ikkeTilgang(SifAbacPdpUtil.hentBegrunnelse(tilgang.get().tilgangsbeslutning().årsakerForIkkeTilgang()));
        }
        ikkeTilgang("Kun systemkall eller ansatt med saksbehandlerrolle støttes.");
    }

    private boolean erNavAnsatt(Kontekst kontekst) {
        return IdentType.InternBruker.equals(kontekst.getIdentType());
    }

    private boolean ansattHarRollen(Kontekst kontekst, AnsattGruppe rolle) {
        return kontekst instanceof RequestKontekst requestKontekst && requestKontekst.harGruppe(rolle);
    }

    private void sjekkErBorger() {
        if (KontekstHolder.getKontekst() instanceof RequestKontekst rq && IdentType.EksternBruker.equals(rq.getIdentType())) {
            return;
        }
        ikkeTilgang("Kun borger kall støttes.");
    }

    private void sjekkBorgersAltinnTilgangTilOrganisasjon(Set<String> organisasjoner) {
        if (organisasjoner.isEmpty()) {
            ikkeTilgang("Mangler informasjon om bedrift.");
        } else {
            for (var orgNr : organisasjoner) {
                if (altinnTilgangTjeneste.manglerTilgangTilBedriften(orgNr)) {
                    ikkeTilgang("Bruker mangler tilgang til bedriften i Altinn.");
                }
            }
        }
    }

    private static void ikkeTilgang(String begrunnelse) {
        LOG.info("Fikk ikke tilgang pga: {}", begrunnelse);
        throw new ManglerTilgangException("IM-00403", String.format("Mangler tilgang til tjenesten. %s", begrunnelse));
    }

}
