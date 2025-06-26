package no.nav.familie.inntektsmelding.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

import static java.time.format.DateTimeFormatter.ofPattern;

public final class FormatUtils {

    private static final Locale NORSK = Locale.forLanguageTag("NO");

    private FormatUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static String formaterPersonnummer(String personnummer) {
        if (personnummer != null && personnummer.length() == 11) {
            var formatertPersonnummer = new StringBuilder(personnummer);
            formatertPersonnummer.insert(6, " ");
            return formatertPersonnummer.toString();
        }
        return personnummer;
    }

    public static String formaterDatoForLister(LocalDate dato) {
        if (dato == null) {
            return null;
        }
        return dato.format(ofPattern("dd.MM.yyyy", NORSK));
    }

    public static String formaterDatoMedNavnPåUkedag(LocalDate dato) {
        if (dato == null) {
            return null;
        }
        var navnPåUkedag = dato.getDayOfWeek().getDisplayName(TextStyle.FULL, NORSK);
        navnPåUkedag = navnPåUkedag.substring(0,1).toUpperCase() + navnPåUkedag.substring(1);
        return navnPåUkedag + " " + dato.format(ofPattern("d. MMMM yyyy", NORSK));
    }

    public static String formaterDatoOgTidNorsk(LocalDateTime opprettetTidspunkt) {
        if (opprettetTidspunkt == null) {
            return null;
        }
        return opprettetTidspunkt.format(ofPattern("d. MMMM yyyy HH:mm:ss", NORSK));
    }
}
