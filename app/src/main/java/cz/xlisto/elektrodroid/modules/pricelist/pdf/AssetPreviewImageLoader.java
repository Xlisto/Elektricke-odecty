package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import cz.xlisto.elektrodroid.R;


/**
 * Asynchronní správce načítání náhledových obrázků PDF ceníků ze složky {@code assets}.
 * <p>
 * Zajišťuje:
 * <ul>
 *     <li>dekódování zmenšených náhledů pro seznam pomocí {@link BitmapFactory.Options#inSampleSize},</li>
 *     <li>omezenou paměťovou mezipaměť {@link LruCache} pouze pro náhledy v seznamu,</li>
 *     <li>načítání plného rozlišení až pro detailní prohlížeč (bez ukládání do sdílené cache),</li>
 *     <li>ochranu proti zobrazení zastaralého obrázku při recyklaci položek {@code RecyclerView},</li>
 *     <li>bezpečné ošetření chybějících nebo poškozených souborů v {@code assets}.</li>
 * </ul>
 */
public final class AssetPreviewImageLoader {

    private static final String TAG = "AssetPreviewImageLoader";
    private static final int MAX_THUMBNAIL_CACHE_KB = 12 * 1024; // Max 12 MB pro náhledy seznamu
    private static final int DEFAULT_MAX_FULL_DIMENSION_PX = 4096;

    private static volatile AssetPreviewImageLoader instance;

    private final LruCache<String, Bitmap> thumbnailCache;
    private final ExecutorService executor;
    private final Handler mainHandler;


    /**
     * Posluchač průběhu načítání náhledu do {@link ImageView}.
     */
    public interface LoadCallback {

        /**
         * Voláno při zahájení asynchronního načítání (při cache miss).
         */
        void onLoading();

        /**
         * Voláno po úspěšném načtení a přiřazení obrázku.
         */
        void onSuccess();

        /**
         * Voláno, pokud soubor chybí nebo jej nelze dekódovat.
         */
        void onError();

    }


    /**
     * Posluchač načtení obrázku v plném rozlišení pro detailní prohlížeč.
     */
    public interface FullBitmapCallback {

        /**
         * Voláno po úspěšném dekódování obrázku na hlavním vlákně.
         *
         * @param bitmap Dekódovaná bitmapa
         */
        void onSuccess(@NonNull Bitmap bitmap);

        /**
         * Voláno při chybě čtení nebo dekódování obrázku na hlavním vlákně.
         */
        void onError();

    }


    private AssetPreviewImageLoader() {
        int maxMemoryKb = (int) (Runtime.getRuntime().maxMemory() / 1024L);
        int cacheSizeKb = Math.max(2048, Math.min(maxMemoryKb / 12, MAX_THUMBNAIL_CACHE_KB));

        this.thumbnailCache = new LruCache<>(cacheSizeKb) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
                return Math.max(1, value.getAllocationByteCount() / 1024);
            }
        };

        this.executor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "AssetPreviewDecoder");
            thread.setDaemon(true);
            return thread;
        });
        this.mainHandler = new Handler(Looper.getMainLooper());
    }


    /**
     * Vrátí sdílenou instanci {@link AssetPreviewImageLoader}.
     *
     * @return singleton instance loaderu
     */
    @NonNull
    public static AssetPreviewImageLoader getInstance() {
        if (instance == null) {
            synchronized (AssetPreviewImageLoader.class) {
                if (instance == null) {
                    instance = new AssetPreviewImageLoader();
                }
            }
        }
        return instance;
    }


    /**
     * Načte zmenšený náhled obrázku z {@code assets} do zadaného {@link ImageView}.
     *
     * @param context     Kontext aplikace
     * @param assetPath   Relativní cesta k obrázku v {@code assets}
     * @param target      Cílový {@link ImageView}
     * @param reqWidthPx  Požadovaná cílová šířka v pixelech
     * @param reqHeightPx Požadovaná cílová výška v pixelech
     * @param callback    Volitelný posluchač stavu načítání
     */
    public void loadThumbnail(
            @NonNull Context context,
            @NonNull String assetPath,
            @NonNull ImageView target,
            int reqWidthPx,
            int reqHeightPx,
            @Nullable LoadCallback callback
    ) {
        int safeWidth = Math.max(120, reqWidthPx);
        int safeHeight = Math.max(120, reqHeightPx);
        String cacheKey = assetPath + "@" + safeWidth + "x" + safeHeight;

        target.setTag(R.id.tag_asset_preview_request_key, cacheKey);

        Bitmap cached = thumbnailCache.get(cacheKey);
        if (cached != null && !cached.isRecycled()) {
            target.setImageBitmap(cached);
            if (callback != null) {
                callback.onSuccess();
            }
            return;
        }

        target.setImageDrawable(null);
        if (callback != null) {
            callback.onLoading();
        }

        Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            Bitmap decoded = decodeSampledFromAssets(appContext.getAssets(), assetPath, safeWidth, safeHeight);
            if (decoded != null) {
                thumbnailCache.put(cacheKey, decoded);
            }

            mainHandler.post(() -> {
                Object currentTag = target.getTag(R.id.tag_asset_preview_request_key);
                if (!cacheKey.equals(currentTag)) {
                    return;
                }
                if (decoded != null && !decoded.isRecycled()) {
                    target.setImageBitmap(decoded);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                } else {
                    target.setImageDrawable(null);
                    if (callback != null) {
                        callback.onError();
                    }
                }
            });
        });
    }


    /**
     * Zruší vazbu cílového {@link ImageView} na rozpracovaný požadavek při recyklaci položky.
     *
     * @param target Recyklovaný {@link ImageView}
     */
    public void clearTarget(@NonNull ImageView target) {
        target.setTag(R.id.tag_asset_preview_request_key, null);
        target.setImageDrawable(null);
    }


    /**
     * Načte obrázek z {@code assets} v plném rozlišení (s bezpečnostním stropem proti překročení
     * limitu GPU textury) mimo hlavní vlákno. Výsledek se neukládá do sdílené cache seznamu.
     *
     * @param context        Kontext aplikace
     * @param assetPath      Relativní cesta k obrázku v {@code assets}
     * @param maxDimensionPx Maximální povolený rozměr delší strany v pixelech (např. 4096)
     * @param callback       Posluchač výsledku na hlavním vlákně
     * @return objekt {@link Future}, pomocí kterého lze načítání zrušit
     */
    @NonNull
    public Future<?> loadFullResolution(
            @NonNull Context context,
            @NonNull String assetPath,
            int maxDimensionPx,
            @NonNull FullBitmapCallback callback
    ) {
        int safeMaxDim = maxDimensionPx > 0 ? maxDimensionPx : DEFAULT_MAX_FULL_DIMENSION_PX;
        Context appContext = context.getApplicationContext();

        return executor.submit(() -> {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            Bitmap decoded = decodeFullFromAssets(appContext.getAssets(), assetPath, safeMaxDim);
            if (Thread.currentThread().isInterrupted()) {
                if (decoded != null && !decoded.isRecycled()) {
                    decoded.recycle();
                }
                return;
            }
            mainHandler.post(() -> {
                if (decoded != null && !decoded.isRecycled()) {
                    callback.onSuccess(decoded);
                } else {
                    callback.onError();
                }
            });
        });
    }


    /**
     * Dekóduje obrázek z {@code assets} s podvzorkováním odpovídajícím cílové velikosti.
     */
    @Nullable
    private static Bitmap decodeSampledFromAssets(
            @NonNull AssetManager assets,
            @NonNull String assetPath,
            int reqWidth,
            int reqHeight
    ) {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;

        try (InputStream boundsStream = assets.open(assetPath)) {
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Nepodařilo se přečíst rozměry náhledu z assets: " + assetPath, e);
            return null;
        }

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return null;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = calculateInSampleSize(
                boundsOptions.outWidth,
                boundsOptions.outHeight,
                reqWidth,
                reqHeight
        );
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try (InputStream imageStream = assets.open(assetPath)) {
            return BitmapFactory.decodeStream(imageStream, null, decodeOptions);
        } catch (IOException | OutOfMemoryError | SecurityException e) {
            Log.w(TAG, "Nepodařilo se dekódovat náhled z assets: " + assetPath, e);
            return null;
        }
    }


    /**
     * Dekóduje obrázek v plném rozlišení se zaručením, že žádná strana nepřekročí {@code maxDimensionPx}.
     */
    @Nullable
    private static Bitmap decodeFullFromAssets(
            @NonNull AssetManager assets,
            @NonNull String assetPath,
            int maxDimensionPx
    ) {
        BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
        boundsOptions.inJustDecodeBounds = true;

        try (InputStream boundsStream = assets.open(assetPath)) {
            BitmapFactory.decodeStream(boundsStream, null, boundsOptions);
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Nepodařilo se přečíst rozměry plného náhledu z assets: " + assetPath, e);
            return null;
        }

        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return null;
        }

        int inSampleSize = 1;
        while ((boundsOptions.outWidth / inSampleSize) > maxDimensionPx
                || (boundsOptions.outHeight / inSampleSize) > maxDimensionPx) {
            inSampleSize *= 2;
        }

        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        decodeOptions.inSampleSize = inSampleSize;
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try (InputStream imageStream = assets.open(assetPath)) {
            return BitmapFactory.decodeStream(imageStream, null, decodeOptions);
        } catch (OutOfMemoryError oom) {
            Log.w(TAG, "Nedostatek paměti při dekódování plného náhledu, zkouším poloviční rozlišení: " + assetPath, oom);
            decodeOptions.inSampleSize = inSampleSize * 2;
            try (InputStream retryStream = assets.open(assetPath)) {
                return BitmapFactory.decodeStream(retryStream, null, decodeOptions);
            } catch (IOException | OutOfMemoryError | SecurityException e) {
                Log.w(TAG, "Opakované dekódování náhledu selhalo: " + assetPath, e);
                return null;
            }
        } catch (IOException | SecurityException e) {
            Log.w(TAG, "Nepodařilo se dekódovat plný náhled z assets: " + assetPath, e);
            return null;
        }
    }


    /**
     * Vypočítá nejvyšší mocninu dvou pro {@link BitmapFactory.Options#inSampleSize},
     * která zachová dostatečné rozlišení pro požadované rozměry cílového pohledu.
     *
     * @param rawWidth  Původní šířka obrázku v pixelech
     * @param rawHeight Původní výška obrázku v pixelech
     * @param reqWidth  Požadovaná šířka v pixelech
     * @param reqHeight Požadovaná výška v pixelech
     * @return Hodnota {@code inSampleSize} (&gt;= 1, mocnina dvou)
     */
    static int calculateInSampleSize(int rawWidth, int rawHeight, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (rawHeight > reqHeight || rawWidth > reqWidth) {
            int halfHeight = rawHeight / 2;
            int halfWidth = rawWidth / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

}
