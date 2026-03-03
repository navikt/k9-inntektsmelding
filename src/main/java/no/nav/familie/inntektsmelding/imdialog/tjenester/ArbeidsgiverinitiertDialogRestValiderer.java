package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.List;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.integrasjoner.k9sak.FagsakInfo;
import no.nav.familie.inntektsmelding.integrasjoner.k9sak.K9SakTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonInfo;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.dto.PeriodeDto;
import no.nav.familie.inntektsmelding.typer.dto.YtelseTypeDto;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.exception.FunksjonellException;

@Dependent
public class ArbeidsgiverinitiertDialogRestValiderer {

    private final GrunnlagTjeneste grunnlagTjeneste;
    private final K9SakTjeneste k9SakTjeneste;


    @Inject
    public ArbeidsgiverinitiertDialogRestValiderer(GrunnlagTjeneste grunnlagTjeneste, K9SakTjeneste k9SakTjeneste) {
        this.grunnlagTjeneste = grunnlagTjeneste;
        this.k9SakTjeneste = k9SakTjeneste;
    }

    public void validerSakIK9(PersonInfo personInfo, YtelseTypeDto ytelseType, LocalDate førsteFraværsdag) {
        // Sjekk at søker har sak i k9-sak
        Ytelsetype ytelsetype = KodeverkMapper.mapYtelsetype(ytelseType);
        AktørId aktørId = new AktørId(personInfo.aktørId().getAktørId());
        List<FagsakInfo> fagsakerIK9Sak =  k9SakTjeneste.hentFagsakInfo(ytelsetype, aktørId);
        List<PeriodeDto> søknadsPerioderForFagsakerIK9 = fagsakerIK9Sak.stream()
            .flatMap(fagsak -> fagsak.søknadsPerioder().stream())
            .toList();

        var finnesSakIK9 = søknadsPerioderForFagsakerIK9.stream()
            .anyMatch(søknandsperiode -> søknandsperiode.inneholderDato(førsteFraværsdag));

        if (!finnesSakIK9) {
            var feilmelding = String.format("Du kan ikke sende inn inntektsmelding på %s for denne personen", ytelsetype);
            throw new FunksjonellException("INGEN_SAK_FUNNET", feilmelding, null, null);
        }

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
