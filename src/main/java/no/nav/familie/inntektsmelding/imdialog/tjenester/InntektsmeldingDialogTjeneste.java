package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;

@ApplicationScoped
public class InntektsmeldingDialogTjeneste {
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;

    InntektsmeldingDialogTjeneste() {
    }

    @Inject
    public InntektsmeldingDialogTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         InntektsmeldingRepository inntektsmeldingRepository,
                                         PersonTjeneste personTjeneste,
                                         OrganisasjonTjeneste organisasjonTjeneste,
                                         InntektTjeneste inntektTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
    }

    public void mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var foresporselUuid = UUID.fromString(mottattInntektsmeldingDto.foresporselUuid());
        var aktorId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto);
        inntektsmeldingRepository.lagreInntektsmelding(entitet);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(foresporselUuid, aktorId, orgnummer, mottattInntektsmeldingDto.startdato());
        // TODO: lagre i database og opprette prosesstask for å lagre i joark
    }

    public InntektsmeldingDialogDto lagDialogDto(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(() -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));
        var personDto = lagPersonDto(forespørsel);
        var organisasjonDto = lagOrganisasjonDto(forespørsel);
        var inntektDtoer = lagInntekterDto(forespørsel);
        return new InntektsmeldingDialogDto(personDto, organisasjonDto, inntektDtoer, forespørsel.getSkjæringstidspunkt(), KodeverkMapper.mapYtelsetype(forespørsel.getYtelseType()));
    }

    private List<InntektsmeldingDialogDto.MånedsinntektResponsDto> lagInntekterDto(ForespørselEntitet forespørsel) {
        var inntekter = inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(),
            forespørsel.getOrganisasjonsnummer());
        var inntektDtoer = inntekter.stream()
            .map(i -> new InntektsmeldingDialogDto.MånedsinntektResponsDto(i.måned().atDay(1), i.måned().atEndOfMonth(), i.beløp()))
            .toList();
        return inntektDtoer;
    }

    private InntektsmeldingDialogDto.OrganisasjonInfoResponseDto lagOrganisasjonDto(ForespørselEntitet forespørsel) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer());
        var organisasjonDto = new InntektsmeldingDialogDto.OrganisasjonInfoResponseDto(orgdata.navn(), orgdata.orgnr());
        return organisasjonDto;
    }

    private InntektsmeldingDialogDto.PersonInfoResponseDto lagPersonDto(ForespørselEntitet forespørsel) {
        var persondata = personTjeneste.hentPersonInfo(forespørsel.getAktørId(), forespørsel.getYtelseType());
        var personDto = new InntektsmeldingDialogDto.PersonInfoResponseDto(persondata.navn(), persondata.fødselsnummer().getIdent(), persondata.aktørId().getAktørId());
        return personDto;
    }

}
