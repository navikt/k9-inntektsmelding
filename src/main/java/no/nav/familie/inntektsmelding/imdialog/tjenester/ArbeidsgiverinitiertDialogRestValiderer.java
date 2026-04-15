package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.imdialog.rest.HentArbeidsforholdResponse;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.FagsakInfo;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.vedtak.exception.FunksjonellException;

@Dependent
public class ArbeidsgiverinitiertDialogRestValiderer {

    private final GrunnlagTjeneste grunnlagTjeneste;


    @Inject
    public ArbeidsgiverinitiertDialogRestValiderer(GrunnlagTjeneste grunnlagTjeneste) {
        this.grunnlagTjeneste = grunnlagTjeneste;
    }

    public void validerPerson(PersonInfo personInfo) {
        if (personInfo == null) {
            throw new FunksjonellException("PERSON_IKKE_FUNNET", "Fant ikke person i pdl", null, null);
        }
    }

    public void validerArbeidsforhold(Optional<HentArbeidsforholdResponse> arbeidsforhold) {
        if (arbeidsforhold.isEmpty()) {
            throw new FunksjonellException("INGEN_ARBEIDSFORHOLD", "Fant ingen arbeidsforhold på brukeren", null, null);
        }
    }

    public void validerArbeidsforhold(HentArbeidsforholdResponse response) {
        if (response != null && response.arbeidsforhold().isEmpty()) {
            throw new FunksjonellException("INGEN_ARBEIDSFORHOLD", "Fant ingen arbeidsforhold på brukeren", null, null);
        }
    }

    public void validerAtFraværsdatoTrefferEnSøknadsperiodeIK9(LocalDate førsteFraværsdag, List<FagsakInfo> fagsakerIK9, Ytelsetype ytelsetype) {
        List<PeriodeDto> søknadsPerioderForFagsakerIK9 = fagsakerIK9.stream()
            .flatMap(fagsak -> fagsak.søknadsPerioder().stream())
            .toList();

        boolean fraværsdagErInnenforSøknadsperiode = søknadsPerioderForFagsakerIK9.stream()
            .anyMatch(søknandsperiode -> søknandsperiode.inneholderDato(førsteFraværsdag));

        if (!fraværsdagErInnenforSøknadsperiode) {
            var feilmelding = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen", ytelsetype);
            throw new FunksjonellException("INGEN_SAK_FUNNET", feilmelding, null, null);
        }
    }

    public void validerAtFraværsdatoErFørsteFraværsdagISøknadsperiode(LocalDate førsteFraværsdag, List<FagsakInfo> fagsakerIK9, Ytelsetype ytelsetype) {
        List<PeriodeDto> søknadsPerioderForFagsakerIK9 = fagsakerIK9.stream()
            .flatMap(fagsak -> fagsak.søknadsPerioder().stream())
            .toList();

        boolean erFørsteFraværsdag = søknadsPerioderForFagsakerIK9.stream()
            .anyMatch(søknandsperiode -> søknandsperiode.fom().isEqual(førsteFraværsdag));

        if (!erFørsteFraværsdag) {
            var feilmelding = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen", ytelsetype);
            throw new FunksjonellException("INGEN_SAK_FUNNET", feilmelding, null, null);
        }
    }

    public void validerAtSakIkkeVenterPåForTidligSøknad(List<FagsakInfo> fagsakerIK9Sak, Ytelsetype ytelsetype) {
        if (fagsakerIK9Sak.stream().anyMatch(FagsakInfo::venterForTidligSøknad)) {
            var feilmelding = String.format("Du kan ikke sende inn inntektsmelding før fire uker før denne personen starter med %s", ytelsetype);
            throw new FunksjonellException("SENDT_FOR_TIDLIG", feilmelding, null, null);
        }
    }

    public void validerAtOrgnummerIkkeFinnesIAaregPåPerson(PersonInfo personInfo, OrganisasjonsnummerDto organisasjonsnummer, LocalDate førsteFraværsdag) {
        var finnesOrgnummerIAaReg = grunnlagTjeneste.finnesOrgnummerIAaregPåPerson(personInfo.fødselsnummer(), organisasjonsnummer.orgnr(), førsteFraværsdag);
        if (finnesOrgnummerIAaReg) {
            var tekst = "Det finnes rapportering i aa-registeret på organisasjonsnummeret. Nav vil be om inntektsmelding når vi trenger det";
            throw new FunksjonellException("FINNES_I_AAREG", tekst, null, null);
        }
    }
}
