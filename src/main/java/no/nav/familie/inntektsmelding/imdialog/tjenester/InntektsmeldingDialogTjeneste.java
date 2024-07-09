package no.nav.familie.inntektsmelding.imdialog.tjenester;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.forespørsel.modell.ForespørselEntitet;
import no.nav.familie.inntektsmelding.forespørsel.tjenester.ForespørselBehandlingTjeneste;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingRepository;
import no.nav.familie.inntektsmelding.imdialog.rest.InntektsmeldingDialogDto;
import no.nav.familie.inntektsmelding.imdialog.rest.SendInntektsmeldingRequestDto;
import no.nav.familie.inntektsmelding.imdialog.task.SendTilJoarkTask;
import no.nav.familie.inntektsmelding.integrasjoner.inntektskomponent.InntektTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.organisasjon.OrganisasjonTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.koder.Ytelsetype;
import no.nav.familie.inntektsmelding.typer.dto.KodeverkMapper;
import no.nav.familie.inntektsmelding.typer.dto.OrganisasjonsnummerDto;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.sikkerhet.kontekst.IdentType;
import no.nav.vedtak.sikkerhet.kontekst.KontekstHolder;

@ApplicationScoped
public class InntektsmeldingDialogTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(InntektsmeldingDialogTjeneste.class);
    private ForespørselBehandlingTjeneste forespørselBehandlingTjeneste;
    private InntektsmeldingRepository inntektsmeldingRepository;
    private PersonTjeneste personTjeneste;
    private OrganisasjonTjeneste organisasjonTjeneste;
    private InntektTjeneste inntektTjeneste;
    private ProsessTaskTjeneste prosessTaskTjeneste;

    InntektsmeldingDialogTjeneste() {
    }

    @Inject
    public InntektsmeldingDialogTjeneste(ForespørselBehandlingTjeneste forespørselBehandlingTjeneste,
                                         InntektsmeldingRepository inntektsmeldingRepository,
                                         PersonTjeneste personTjeneste,
                                         OrganisasjonTjeneste organisasjonTjeneste,
                                         InntektTjeneste inntektTjeneste,
                                         ProsessTaskTjeneste prosessTaskTjeneste) {
        this.forespørselBehandlingTjeneste = forespørselBehandlingTjeneste;
        this.inntektsmeldingRepository = inntektsmeldingRepository;
        this.personTjeneste = personTjeneste;
        this.organisasjonTjeneste = organisasjonTjeneste;
        this.inntektTjeneste = inntektTjeneste;
        this.prosessTaskTjeneste = prosessTaskTjeneste;
    }

    public void mottaInntektsmelding(SendInntektsmeldingRequestDto mottattInntektsmeldingDto) {
        var aktorId = new AktørIdEntitet(mottattInntektsmeldingDto.aktorId().id());
        var orgnummer = new OrganisasjonsnummerDto(mottattInntektsmeldingDto.arbeidsgiverIdent().ident());
        var entitet = InntektsmeldingMapper.mapTilEntitet(mottattInntektsmeldingDto);
        var imId = inntektsmeldingRepository.lagreInntektsmelding(entitet);

        forespørselBehandlingTjeneste.ferdigstillForespørsel(mottattInntektsmeldingDto.foresporselUuid(), aktorId, orgnummer,
            mottattInntektsmeldingDto.startdato());
        opprettTaskForSendTilJoark(imId);
    }

    private void opprettTaskForSendTilJoark(Long imId) {
        var task = ProsessTaskData.forProsessTask(SendTilJoarkTask.class);
        task.setProperty(SendTilJoarkTask.KEY_INNTEKTSMELDING_ID, imId.toString());
        task.setCallIdFraEksisterende();
        prosessTaskTjeneste.lagre(task);
        LOG.info("Opprettet task for oversending til joark");
    }

    public InntektsmeldingDialogDto lagDialogDto(UUID forespørselUuid) {
        var forespørsel = forespørselBehandlingTjeneste.hentForespørsel(forespørselUuid)
            .orElseThrow(
                () -> new IllegalStateException("Prøver å hente data for en forespørsel som ikke finnes, forespørselUUID: " + forespørselUuid));
        var personDto = lagPersonDto(forespørsel);
        var organisasjonDto = lagOrganisasjonDto(forespørsel);
        var innmelderDto = lagInnmelderDto(forespørsel.getYtelseType());
        var inntektDtoer = lagInntekterDto(forespørsel);
        return new InntektsmeldingDialogDto(personDto, organisasjonDto, innmelderDto, inntektDtoer, forespørsel.getSkjæringstidspunkt(),
            KodeverkMapper.mapYtelsetype(forespørsel.getYtelseType()), forespørsel.getUuid());
    }

    public InntektsmeldingEntitet hentInntektsmelding(int inntektsmeldingId) {
        return inntektsmeldingRepository.hentInntektsmelding(inntektsmeldingId);
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

    private List<InntektsmeldingDialogDto.MånedsinntektResponsDto> lagInntekterDto(ForespørselEntitet forespørsel) {
        var inntekter = inntektTjeneste.hentInntekt(forespørsel.getAktørId(), forespørsel.getSkjæringstidspunkt(),
            forespørsel.getOrganisasjonsnummer());
        return inntekter.stream()
            .map(i -> new InntektsmeldingDialogDto.MånedsinntektResponsDto(i.måned().atDay(1), i.måned().atEndOfMonth(), i.beløp(),
                forespørsel.getOrganisasjonsnummer()))
            .toList();
    }

    private InntektsmeldingDialogDto.OrganisasjonInfoResponseDto lagOrganisasjonDto(ForespørselEntitet forespørsel) {
        var orgdata = organisasjonTjeneste.finnOrganisasjon(forespørsel.getOrganisasjonsnummer());
        return new InntektsmeldingDialogDto.OrganisasjonInfoResponseDto(orgdata.navn(), orgdata.orgnr());
    }

    private InntektsmeldingDialogDto.PersonInfoResponseDto lagPersonDto(ForespørselEntitet forespørsel) {
        var persondata = personTjeneste.hentPersonInfoFraAktørId(forespørsel.getAktørId(), forespørsel.getYtelseType());
        return new InntektsmeldingDialogDto.PersonInfoResponseDto(persondata.fornavn(), persondata.mellomnavn(), persondata.etternavn(),
            persondata.fødselsnummer().getIdent(), persondata.aktørId().getAktørId());
    }

}
