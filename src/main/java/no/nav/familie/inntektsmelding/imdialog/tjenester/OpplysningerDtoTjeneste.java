package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class OpplysningerDtoTjeneste {
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;

    OpplysningerDtoTjeneste() {
    }

    @Inject
    public OpplysningerDtoTjeneste(PersonTjeneste personTjeneste,
                                   OrganisasjonTjeneste organisasjonTjeneste,
                                   InntektTjeneste inntektTjeneste) {
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
    }

    public InntektsmeldingDialogDto lagOpplysningerDto(ForespørselEntitet forespørsel) {
        var personDto = lagPersonDto(forespørsel);
        var organisasjonDto = lagOrganisasjonDto(forespørsel);
        var innmelderDto = lagInnmelderDto(forespørsel.getYtelseType());
        var inntektDtoer = lagInntekterDto(forespørsel);
        var søknadsopplysninger = utledSøknadsopplysninger(forespørsel);
        return new InntektsmeldingDialogDto(personDto, organisasjonDto, innmelderDto, inntektDtoer, forespørsel.getSkjæringstidspunkt(),
            KodeverkMapper.mapYtelsetype(forespørsel.getYtelseType()), forespørsel.getUuid(), KodeverkMapper.mapForespørselStatus(forespørsel.getStatus()), søknadsopplysninger.orElse(null));
    }

    private Optional<InntektsmeldingDialogDto.SøknadsopplysningerDto> utledSøknadsopplysninger(ForespørselEntitet forespørsel) {
        Optional<LocalDate> førsteFom = forespørsel.getSøknadsperioder().stream().map(s -> s.getPeriode().getFom()).min(Comparator.naturalOrder());
        if (førsteFom.isEmpty()) {
            return Optional.empty();
        }
        Optional<LocalDate> sistTom = forespørsel.getSøknadsperioder().stream().map(s -> s.getPeriode().getTom()).max(Comparator.naturalOrder());
        List<InntektsmeldingDialogDto.SøknadsperioderDto> allePerioder = forespørsel.getSøknadsperioder()
            .stream()
            .map(s -> new InntektsmeldingDialogDto.SøknadsperioderDto(s.getPeriode().getFom(), s.getPeriode().getTom()))
            .toList();
        return Optional.of(new InntektsmeldingDialogDto.SøknadsopplysningerDto(førsteFom.orElse(null), sistTom.orElse(null), allePerioder));
    }

    private InntektsmeldingDialogDto.InnsenderDto lagInnmelderDto(Ytelsetype ytelsetype) {
        if (!KontekstHolder.harKontekst() || !IdentType.EksternBruker.equals(KontekstHolder.getKontekst().getIdentType())) {
            throw new IllegalStateException("Mangler innlogget bruker kontekst.");
        }
        var pid = KontekstHolder.getKontekst().getUid();
        var personInfo = personTjeneste.hentPersonFraIdent(PersonIdent.fra(pid), ytelsetype);
        return new InntektsmeldingDialogDto.InnsenderDto(personInfo.fornavn(), personInfo.mellomnavn(), personInfo.etternavn(),
            personInfo.telefonnummer());
    }

    private InntektsmeldingDialogDto.InntektsopplysningerDto lagInntekterDto(ForespørselEntitet forespørsel) {
        var inntektsopplysninger = inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(), LocalDate.now(),
            forespørsel.getOrganisasjonsnummer());
        var inntekter = inntektsopplysninger.måneder()
            .stream()
            .map(i -> new InntektsmeldingDialogDto.InntektsopplysningerDto.MånedsinntektDto(i.månedÅr().atDay(1),
                i.månedÅr().atEndOfMonth(),
                i.beløp(),
                i.status()))
            .toList();
        return new InntektsmeldingDialogDto.InntektsopplysningerDto(inntektsopplysninger.gjennomsnitt(), inntekter);
    }

    private InntektsmeldingDialogDto.OrganisasjonInfoDto lagOrganisasjonDto(ForespørselEntitet forespørsel) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer());
        return new InntektsmeldingDialogDto.OrganisasjonInfoDto(orgdata.navn(), orgdata.orgnr());
    }

    private InntektsmeldingDialogDto.PersonInfoDto lagPersonDto(ForespørselEntitet forespørsel) {
        var persondata = personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType());
        return new InntektsmeldingDialogDto.PersonInfoDto(persondata.fornavn(), persondata.mellomnavn(), persondata.etternavn(),
            persondata.fødselsnummer().getIdent(), persondata.aktørId().getAktørId());
    }
}
