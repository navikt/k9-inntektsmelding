package no.nav.familie.inntektsmelding.imdialog.task;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.familie.inntektsmelding.imdialog.modell.InntektsmeldingEntitet;
import no.nav.familie.inntektsmelding.imdialog.tjenester.InntektsmeldingDialogTjeneste;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonIdent;
import no.nav.familie.inntektsmelding.integrasjoner.person.PersonTjeneste;
import no.nav.familie.inntektsmelding.typer.OrganisasjonsnummerValidator;
import no.nav.familie.inntektsmelding.typer.entitet.AktørIdEntitet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

import no.seres.xsd.nav.inntektsmelding_m._20181211.Arbeidsgiver;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ArbeidsgiverPrivat;
import no.seres.xsd.nav.inntektsmelding_m._20181211.InntektsmeldingM;

import no.seres.xsd.nav.inntektsmelding_m._20181211.Kontaktinformasjon;
import no.seres.xsd.nav.inntektsmelding_m._20181211.ObjectFactory;
import no.seres.xsd.nav.inntektsmelding_m._20181211.Skjemainnhold;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@ApplicationScoped
@ProsessTask(value = "mottaInntektsmelding.oversendJoark")
public class SendTilJoarkTask implements ProsessTaskHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SendTilJoarkTask.class);
    public static final String KEY_INNTEKTSMELDING_ID = "inntektsmeldingId";
    private InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste;
    private PersonTjeneste personTjeneste;

    SendTilJoarkTask() {
        // CDI
    }

    @Inject
    public SendTilJoarkTask(InntektsmeldingDialogTjeneste inntektsmeldingDialogTjeneste,
                            PersonTjeneste personTjeneste) {
        this.inntektsmeldingDialogTjeneste = inntektsmeldingDialogTjeneste;
        this.personTjeneste = personTjeneste;

    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {
        var inntektsmeldingId = Integer.parseInt(prosessTaskData.getPropertyValue(KEY_INNTEKTSMELDING_ID));
        var inntektsmelding = inntektsmeldingDialogTjeneste.hentInntektsmelding(inntektsmeldingId);
        var søkerIdent = personTjeneste.finnPersonIdentForAktørId(inntektsmelding.getAktørId());
        var aktørIdIdentMap = Map.of(inntektsmelding.getAktørId(), søkerIdent);
        if (!OrganisasjonsnummerValidator.erGyldig(inntektsmelding.getArbeidsgiverIdent()) && inntektsmelding.getArbeidsgiverIdent().length() == 13) {
            var arbeidsgiverAktørId = new AktørIdEntitet(inntektsmelding.getArbeidsgiverIdent());
            var arbeidsgiverIdent = personTjeneste.finnPersonIdentForAktørId(arbeidsgiverAktørId);
            aktørIdIdentMap.put(arbeidsgiverAktørId, arbeidsgiverIdent);
        }
        var inntektsmeldingXml = InntektsmeldingXMLMapper.map(inntektsmelding, aktørIdIdentMap);

        InntektsmeldingM test = new InntektsmeldingM();
        var of = new ObjectFactory();
        Skjemainnhold skjemainnhold = new Skjemainnhold();
        if (OrganisasjonsnummerValidator.erGyldig(inntektsmelding.getArbeidsgiverIdent())) {
            var arbeidsgiver = new Arbeidsgiver();
            arbeidsgiver.setVirksomhetsnummer(inntektsmelding.getArbeidsgiverIdent());
            arbeidsgiver.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
            var agOrg = of.createSkjemainnholdArbeidsgiver(arbeidsgiver);
            skjemainnhold.setArbeidsgiver(agOrg);
        } else if (inntektsmelding.getArbeidsgiverIdent().length() == 13) {
            var agPrivat = new ArbeidsgiverPrivat();
            var identArbeidsgiver = personTjeneste.finnPersonIdentForAktørId(new AktørIdEntitet(inntektsmelding.getArbeidsgiverIdent()));
            agPrivat.setArbeidsgiverFnr(identArbeidsgiver.getIdent());
            agPrivat.setKontaktinformasjon(lagKontaktperson(inntektsmelding));
            of.createSkjemainnholdArbeidsgiverPrivat(agPrivat);
        }
        inntektsmelding.getArbeidsgiverIdent().length()
        skjemainnhold.setArbeidsgiverPrivat();
        // TODO fyll med innhold
        LOG.info("kjører task");
    }
}
