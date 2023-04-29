package cz.xlisto.cenik.modules.backup;

import androidx.documentfile.provider.DocumentFile;

/**
 * Xlisto 24.04.2023 21:22
 */
public class FilterNameFile {
    private static final String TAG = "FilterNameFile";
    private String[] mExtensions;

    public FilterNameFile(String[] mExtensions) {
        this.mExtensions = mExtensions;
    }

    public boolean accept(DocumentFile file) {
        if (file.isDirectory()) {
            // kontrola na to zda je to složka
            return false;
        }
        if (mExtensions != null && mExtensions.length > 0) {
            for (int i = 0; i < mExtensions.length; i++) {
                try {
                    if (file.getName().endsWith(mExtensions[i])) {
                        // The filename ends with the extension
                        return true;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            // The filename did not match any of the extensions
            return false;
        }
        // No extensions has been set. Accept all file extensions.
        return true;
    }
}
