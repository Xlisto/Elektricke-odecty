package cz.xlisto.elektrodroid.modules.backup;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;


/**
 * ViewModel pro správu a publikaci stavu hromadného mazání souborů na Google Drive.
 *
 * <p>Třída drží stav operace mazání v {@link LiveData}, aby UI (fragment) mohlo
 * bezpečně reagovat na průběh i po změně konfigurace (např. rotace obrazovky).
 * Stav je reprezentován hodnotou {@link DeleteState} a přechází mezi stavy
 * {@link DeleteStatus#IDLE}, {@link DeleteStatus#IN_PROGRESS},
 * {@link DeleteStatus#FINISHED} a {@link DeleteStatus#FAILED}.</p>
 */
public class GoogleDriveViewModel extends ViewModel {

    /** Stav mazání */
    public enum DeleteStatus {
        IDLE, IN_PROGRESS, FINISHED, FAILED
    }

    /** Stav ukládání do lokálního úložiště */
    public enum SaveStatus {
        IDLE, IN_PROGRESS, FINISHED, FAILED
    }

    public record DeleteState(DeleteStatus status, boolean success, int deletedCount, int totalCount,
                              @Nullable String errorMessage) {

        /**
         * Vrátí výchozí klidový stav bez aktivní operace.
         *
         * @return stav {@link DeleteStatus#IDLE}
         */
        public static DeleteState idle() {
            return new DeleteState(DeleteStatus.IDLE, false, 0, 0, null);
        }

        /**
         * Vrátí stav probíhající operace mazání.
         *
         * @param totalCount celkový počet souborů určených ke smazání
         * @return stav {@link DeleteStatus#IN_PROGRESS}
         */
        public static DeleteState inProgress(int totalCount) {
            return new DeleteState(DeleteStatus.IN_PROGRESS, false, 0, totalCount, null);
        }

        /**
         * Vrátí finální stav po dokončení mazání.
         *
         * @param success      {@code true}, pokud byly smazány všechny požadované soubory
         * @param deletedCount počet úspěšně smazaných souborů
         * @param totalCount   celkový počet souborů, které se měly mazat
         * @return stav {@link DeleteStatus#FINISHED}
         */
        public static DeleteState finished(boolean success, int deletedCount, int totalCount) {
            return new DeleteState(DeleteStatus.FINISHED, success, deletedCount, totalCount, null);
        }

        /**
         * Vrátí chybový stav operace mazání.
         *
         * @param errorMessage text chyby určený pro zobrazení uživateli
         * @return stav {@link DeleteStatus#FAILED}
         */
        public static DeleteState failed(String errorMessage) {
            return new DeleteState(DeleteStatus.FAILED, false, 0, 0, errorMessage);
        }
    }

    public record SaveState(SaveStatus status, boolean success, int processedCount, int savedCount, int totalCount,
                            @Nullable String errorMessage) {

        public static SaveState idle() {
            return new SaveState(SaveStatus.IDLE, false, 0, 0, 0, null);
        }

        public static SaveState inProgress(int processedCount, int savedCount, int totalCount) {
            return new SaveState(SaveStatus.IN_PROGRESS, false, processedCount, savedCount, totalCount, null);
        }

        public static SaveState finished(boolean success, int savedCount, int totalCount) {
            return new SaveState(SaveStatus.FINISHED, success, totalCount, savedCount, totalCount, null);
        }

    }

    private final MutableLiveData<DeleteState> deleteState = new MutableLiveData<>(DeleteState.idle());
    private final MutableLiveData<SaveState> saveState = new MutableLiveData<>(SaveState.idle());

    /**
     * Vrátí lifecycle-aware stream aktuálního stavu mazání.
     *
     * @return {@link LiveData} s hodnotou {@link DeleteState}
     */
    public LiveData<DeleteState> getDeleteState() {
        return deleteState;
    }

    /**
     * Vrátí lifecycle-aware stream aktuálního stavu ukládání do lokálního úložiště.
     */
    public LiveData<SaveState> getSaveState() {
        return saveState;
    }

    /**
     * Nastaví stav na průběh mazání.
     *
     * @param totalCount celkový počet souborů, které budou mazány
     */
    public void setInProgress(int totalCount) {
        deleteState.postValue(DeleteState.inProgress(totalCount));
    }

    /**
     * Nastaví stav na průběh ukládání do lokálního úložiště.
     */
    public void setSaveInProgress(int totalCount) {
        saveState.postValue(SaveState.inProgress(0, 0, totalCount));
    }

    /**
     * Aktualizuje průběh ukládání do lokálního úložiště.
     */
    public void setSaveProgress(int processedCount, int savedCount, int totalCount) {
        saveState.postValue(SaveState.inProgress(processedCount, savedCount, totalCount));
    }

    /**
     * Nastaví výsledný stav po dokončení operace mazání.
     *
     * @param success      {@code true}, pokud byly smazány všechny položky
     * @param deletedCount počet úspěšně smazaných souborů
     * @param totalCount   celkový počet souborů určených ke smazání
     */
    public void setFinished(boolean success, int deletedCount, int totalCount) {
        deleteState.postValue(DeleteState.finished(success, deletedCount, totalCount));
    }

    /**
     * Nastaví výsledný stav po dokončení ukládání do lokálního úložiště.
     */
    public void setSaveFinished(boolean success, int savedCount, int totalCount) {
        saveState.postValue(SaveState.finished(success, savedCount, totalCount));
    }

    /**
     * Nastaví chybový stav operace.
     *
     * @param errorMessage text chyby pro UI vrstvu
     */
    public void setFailed(String errorMessage) {
        deleteState.postValue(DeleteState.failed(errorMessage));
    }


    /**
     * Resetuje stav ViewModelu do výchozího klidového stavu.
     */
    public void resetToIdle() {
        deleteState.postValue(DeleteState.idle());
    }

    /**
     * Resetuje stav ukládání do lokálního úložiště do výchozího stavu.
     */
    public void resetSaveToIdle() {
        saveState.postValue(SaveState.idle());
    }
}

