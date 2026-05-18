package cz.xlisto.elektrodroid.modules.backup;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pomocná třída pro extrakci času zálohy z názvu souboru.
 *
 * <p>Zálohy aplikace začínají datem a časem ve formátu {@code dd.MM.yyyy  HH.mm.ss}.
 * Tento čas používáme jako spolehlivý zdroj pravdy pro řazení i při kopírování přes SAF,
 * kde metadata souboru nemusí být zachována konzistentně.</p>
 */
public final class BackupFileTimestampHelper {

    private static final Pattern BACKUP_DATE_PATTERN = Pattern.compile("^(\\d{2}\\.\\d{2}\\.\\d{4}\\s{2}\\d{2}\\.\\d{2}\\.\\d{2})");
    private static final ThreadLocal<SimpleDateFormat> BACKUP_FILE_DATE_FORMAT = ThreadLocal.withInitial(() -> {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy  HH.mm.ss", Locale.GERMANY);
        simpleDateFormat.setLenient(false);
        return simpleDateFormat;
    });

    private BackupFileTimestampHelper() {
    }

    /**
     * Pokusí se vyparsovat datum a čas ze začátku názvu zálohy.
     *
     * @param fileName název souboru zálohy
     * @return epoch millis nebo {@code null}, pokud název neodpovídá očekávanému formátu
     */
    @Nullable
    public static Long extractTimestampFromName(@Nullable String fileName) {
        if (fileName == null || fileName.trim().isEmpty())
            return null;

        Matcher matcher = BACKUP_DATE_PATTERN.matcher(fileName);
        if (!matcher.find())
            return null;

        String dateText = matcher.group(1);
        if (dateText == null)
            return null;

        SimpleDateFormat simpleDateFormat = BACKUP_FILE_DATE_FORMAT.get();
        if (simpleDateFormat == null)
            return null;

        try {
            java.util.Date parsedDate = simpleDateFormat.parse(dateText);
            return parsedDate != null ? parsedDate.getTime() : null;
        } catch (ParseException e) {
            return null;
        }
    }
}

