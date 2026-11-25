package cz.xlisto.elektrodroid.modules.pricelist.pdf;


import android.app.Dialog;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.DialogFragment;

import java.util.concurrent.Future;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.ownview.ZoomableImageView;


/**
 * Celoobrazovkový prohlížeč náhledového obrázku kompletního PDF ceníku.
 * <p>
 * Zobrazuje obrázek v plném rozlišení (načtený asynchronně ze složky {@code assets}) v komponentě
 * {@link ZoomableImageView} s podporou přiblížení dvěma prsty, dvojitého klepnutí a posouvání.
 * Díky použití {@link DialogFragment} zachovává podkladový seznam v {@link SupportedPriceListsFragment}
 * beze změny pozice posunu po návratu.
 */
public class PriceListPreviewDialogFragment extends DialogFragment {

    /**
     * Tag pro zobrazení dialogu ve {@code FragmentManager}.
     */
    public static final String TAG = "PriceListPreviewDialogFragment";

    private static final String ARG_SUPPLIER_NAME = "argSupplierName";
    private static final String ARG_ASSET_PATH = "argAssetPath";
    private static final String ARG_SAMPLE_PAGES = "argSamplePages";
    private static final int MAX_FULL_IMAGE_DIMENSION_PX = 4096;

    @Nullable
    private ZoomableImageView zoomableImageView;
    @Nullable
    private View progressFullPreview;
    @Nullable
    private View tvFullPreviewError;
    @Nullable
    private View topBarFullPreview;
    @Nullable
    private View bottomHintFullPreview;
    @Nullable
    private View btnResetZoom;

    @Nullable
    private Future<?> pendingLoadTask;
    @Nullable
    private Bitmap loadedBitmap;
    private boolean chromeVisible = true;
    private int lastInsetLeft = 0;
    private int lastInsetRight = 0;
    private int lastInsetTop = 0;
    private int lastInsetBottom = 0;


    /**
     * Vytvoří novou instanci celoobrazovkového prohlížeče náhledu ceníku.
     *
     * @param supplierName    Zobrazovaný název dodavatele
     * @param assetPath       Relativní cesta k obrázku v {@code assets}
     * @param samplePageCount Informativní počet stran ukázkového PDF dokumentu
     * @return Nová instance {@link PriceListPreviewDialogFragment}
     */
    @NonNull
    public static PriceListPreviewDialogFragment newInstance(
            @NonNull String supplierName,
            @NonNull String assetPath,
            int samplePageCount
    ) {
        PriceListPreviewDialogFragment fragment = new PriceListPreviewDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SUPPLIER_NAME, supplierName);
        args.putString(ARG_ASSET_PATH, assetPath);
        args.putInt(ARG_SAMPLE_PAGES, samplePageCount);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.FullscreenPreviewDialogTheme);
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false);
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, window.getDecorView());
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }
        return dialog;
    }


    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.dialog_price_list_preview_fullscreen, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        zoomableImageView = view.findViewById(R.id.zoomableImageView);
        progressFullPreview = view.findViewById(R.id.progressFullPreview);
        tvFullPreviewError = view.findViewById(R.id.tvFullPreviewError);
        topBarFullPreview = view.findViewById(R.id.topBarFullPreview);
        bottomHintFullPreview = view.findViewById(R.id.bottomHintFullPreview);
        btnResetZoom = view.findViewById(R.id.btnResetZoom);

        ImageButton btnClose = view.findViewById(R.id.btnClosePreview);
        TextView tvTitle = view.findViewById(R.id.tvFullPreviewTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvFullPreviewSubtitle);

        Bundle args = getArguments();
        String supplierName = args != null ? args.getString(ARG_SUPPLIER_NAME, "") : "";
        String assetPath = args != null ? args.getString(ARG_ASSET_PATH, "") : "";
        int samplePages = args != null ? args.getInt(ARG_SAMPLE_PAGES, 1) : 1;

        tvTitle.setText(supplierName);
        tvSubtitle.setText(getResources().getQuantityString(
                R.plurals.supported_price_list_sample_pages,
                samplePages,
                samplePages
        ));

        if (zoomableImageView != null) {
            zoomableImageView.setContentDescription(
                    getString(R.string.supported_price_lists_preview_image_cd, supplierName)
            );
            zoomableImageView.setOnZoomChangeListener(zoomFactor -> {
                if (btnResetZoom != null) {
                    btnResetZoom.setVisibility(zoomFactor > 1.05f ? View.VISIBLE : View.GONE);
                }
            });
            zoomableImageView.setOnSingleTapListener(this::toggleChromeVisibility);
        }

        btnClose.setOnClickListener(v -> dismissAllowingStateLoss());
        if (btnResetZoom != null) {
            btnResetZoom.setOnClickListener(v -> {
                if (zoomableImageView != null) {
                    zoomableImageView.resetZoom();
                }
            });
        }

        setupEdgeToEdgeInsets(view);
        loadFullImage(assetPath);
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }


    @Override
    public void onDestroyView() {
        if (pendingLoadTask != null) {
            pendingLoadTask.cancel(true);
            pendingLoadTask = null;
        }
        if (zoomableImageView != null) {
            zoomableImageView.setImageDrawable(null);
            zoomableImageView = null;
        }
        if (loadedBitmap != null && !loadedBitmap.isRecycled()) {
            loadedBitmap.recycle();
            loadedBitmap = null;
        }
        progressFullPreview = null;
        tvFullPreviewError = null;
        topBarFullPreview = null;
        bottomHintFullPreview = null;
        btnResetZoom = null;
        super.onDestroyView();
    }


    /**
     * Nastaví systémové insety pro horní lištu, spodní nápovědu i výchozí zarovnání obrázku.
     */
    private void setupEdgeToEdgeInsets(@NonNull View rootView) {
        float density = getResources().getDisplayMetrics().density;
        int baseHorizontalPx = Math.round(4f * density);
        int baseBottomVerticalPx = Math.round(10f * density);
        int baseBottomHorizontalPx = Math.round(16f * density);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, windowInsets) -> {
            Insets sysBars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            lastInsetLeft = sysBars.left;
            lastInsetTop = sysBars.top;
            lastInsetRight = sysBars.right;
            lastInsetBottom = sysBars.bottom;

            if (topBarFullPreview != null) {
                topBarFullPreview.setPadding(
                        baseHorizontalPx + sysBars.left,
                        sysBars.top,
                        baseHorizontalPx + sysBars.right,
                        0
                );
            }
            if (bottomHintFullPreview != null) {
                bottomHintFullPreview.setPadding(
                        baseBottomHorizontalPx + sysBars.left,
                        baseBottomVerticalPx,
                        baseBottomHorizontalPx + sysBars.right,
                        baseBottomVerticalPx + sysBars.bottom
                );
            }
            rootView.post(this::updateImageFitInsets);
            return windowInsets;
        });

        ViewCompat.requestApplyInsets(rootView);
    }


    /**
     * Aktualizuje bezpečné okraje pro výchozí přizpůsobení dokumentu mezi horní a spodní lištou.
     */
    private void updateImageFitInsets() {
        if (zoomableImageView == null) {
            return;
        }
        float density = getResources().getDisplayMetrics().density;
        int marginPx = Math.round(8f * density);
        int topSpace = (topBarFullPreview != null && topBarFullPreview.getHeight() > 0)
                ? topBarFullPreview.getHeight() + marginPx
                : lastInsetTop + Math.round(64f * density);
        int bottomSpace = (bottomHintFullPreview != null && bottomHintFullPreview.getHeight() > 0)
                ? bottomHintFullPreview.getHeight() + marginPx
                : lastInsetBottom + Math.round(44f * density);

        zoomableImageView.setFitInsets(
                lastInsetLeft + marginPx,
                topSpace,
                lastInsetRight + marginPx,
                bottomSpace
        );
    }


    /**
     * Přepne viditelnost horní a spodní překryvné lišty po jednom klepnutí na dokument.
     */
    private void toggleChromeVisibility() {
        chromeVisible = !chromeVisible;
        float targetAlpha = chromeVisible ? 1.0f : 0.0f;
        int targetVisibility = chromeVisible ? View.VISIBLE : View.GONE;

        animateChromeView(topBarFullPreview, targetAlpha, targetVisibility);
        animateChromeView(bottomHintFullPreview, targetAlpha, targetVisibility);
    }


    private void animateChromeView(@Nullable View view, float targetAlpha, int targetVisibility) {
        if (view == null) {
            return;
        }
        view.animate().cancel();
        if (targetVisibility == View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }
        view.animate()
                .alpha(targetAlpha)
                .setDuration(180L)
                .withEndAction(() -> {
                    if (targetVisibility == View.GONE) {
                        view.setVisibility(View.GONE);
                    }
                })
                .start();
    }


    /**
     * Asynchronně načte náhledový obrázek v plném rozlišení.
     */
    private void loadFullImage(@NonNull String assetPath) {
        if (assetPath.isEmpty()) {
            showErrorState();
            return;
        }

        if (progressFullPreview != null) {
            progressFullPreview.setVisibility(View.VISIBLE);
        }
        if (tvFullPreviewError != null) {
            tvFullPreviewError.setVisibility(View.GONE);
        }

        pendingLoadTask = AssetPreviewImageLoader.getInstance().loadFullResolution(
                requireContext(),
                assetPath,
                MAX_FULL_IMAGE_DIMENSION_PX,
                new AssetPreviewImageLoader.FullBitmapCallback() {
                    @Override
                    public void onSuccess(@NonNull Bitmap bitmap) {
                        if (!isAdded() || zoomableImageView == null) {
                            if (!bitmap.isRecycled()) {
                                bitmap.recycle();
                            }
                            return;
                        }
                        if (loadedBitmap != null && loadedBitmap != bitmap && !loadedBitmap.isRecycled()) {
                            loadedBitmap.recycle();
                        }
                        loadedBitmap = bitmap;
                        if (progressFullPreview != null) {
                            progressFullPreview.setVisibility(View.GONE);
                        }
                        if (tvFullPreviewError != null) {
                            tvFullPreviewError.setVisibility(View.GONE);
                        }
                        zoomableImageView.setImageBitmap(bitmap);
                        updateImageFitInsets();
                    }


                    @Override
                    public void onError() {
                        if (!isAdded()) {
                            return;
                        }
                        showErrorState();
                    }
                }
        );
    }


    private void showErrorState() {
        if (progressFullPreview != null) {
            progressFullPreview.setVisibility(View.GONE);
        }
        if (tvFullPreviewError != null) {
            tvFullPreviewError.setVisibility(View.VISIBLE);
        }
    }

}
