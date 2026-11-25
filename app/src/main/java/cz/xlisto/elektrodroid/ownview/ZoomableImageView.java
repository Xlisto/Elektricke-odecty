package cz.xlisto.elektrodroid.ownview;


import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;


/**
 * Specializovaný {@link AppCompatImageView} pro interaktivní prohlížení detailních obrázků ceníků.
 * <p>
 * Podporuje:
 * <ul>
 *     <li>plynulé zvětšování a zmenšování gestem dvěma prsty (pinch-to-zoom) až do 8× přiblížení,</li>
 *     <li>posouvání zvětšeného dokumentu včetně setrvačného dojezdu (fling),</li>
 *     <li>animované zvětšení / obnovení dvojitým klepnutím,</li>
 *     <li>zachování původního poměru stran a vykreslení bílého pozadí pod dokumentem i v tmavém motivu,</li>
 *     <li>přizpůsobení při změně orientace obrazovky a respektování vizuálních insetů lišt.</li>
 * </ul>
 */
public class ZoomableImageView extends AppCompatImageView {

    private static final float MIN_ZOOM = 1.0f;
    private static final float DOUBLE_TAP_ZOOM = 3.0f;
    private static final float MAX_ZOOM = 8.0f;
    private static final long ZOOM_ANIMATION_DURATION_MS = 220L;

    private final Matrix drawMatrix = new Matrix();
    private final float[] matrixValues = new float[9];
    private final RectF mappedBounds = new RectF();
    private final Paint documentBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final ScaleGestureDetector scaleGestureDetector;
    private final GestureDetector gestureDetector;
    private final OverScroller flingScroller;

    private float baseScale = 1.0f;
    private float currentZoom = MIN_ZOOM;
    private int insetLeft = 0;
    private int insetTop = 0;
    private int insetRight = 0;
    private int insetBottom = 0;
    private int lastFlingX = 0;
    private int lastFlingY = 0;
    private boolean isFlinging = false;

    @Nullable
    private ValueAnimator zoomAnimator;
    @Nullable
    private OnZoomChangeListener onZoomChangeListener;
    @Nullable
    private OnSingleTapListener onSingleTapListener;


    /**
     * Posluchač změny aktuálního zvětšení obrázku.
     */
    public interface OnZoomChangeListener {

        /**
         * Voláno při změně faktoru přiblížení.
         *
         * @param zoomFactor Aktuální zvětšení vůči výchozímu přizpůsobení obrazovce (1.0 = výchozí)
         */
        void onZoomChanged(float zoomFactor);

    }


    /**
     * Posluchač jednoduchého klepnutí na obrázek (např. pro přepnutí viditelnosti lišt).
     */
    public interface OnSingleTapListener {

        /**
         * Voláno po potvrzení jednoho klepnutí na obrázek.
         */
        void onSingleTap();

    }


    private final Runnable flingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isFlinging) {
                return;
            }
            if (flingScroller.computeScrollOffset()) {
                int currX = flingScroller.getCurrX();
                int currY = flingScroller.getCurrY();
                int dx = currX - lastFlingX;
                int dy = currY - lastFlingY;
                lastFlingX = currX;
                lastFlingY = currY;

                drawMatrix.postTranslate(dx, dy);
                constrainTranslation();
                setImageMatrix(drawMatrix);
                postOnAnimation(this);
            } else {
                isFlinging = false;
            }
        }
    };


    /**
     * Vytvoří instanci {@link ZoomableImageView} programově.
     *
     * @param context Kontext aplikace
     */
    public ZoomableImageView(@NonNull Context context) {
        this(context, null);
    }


    /**
     * Vytvoří instanci {@link ZoomableImageView} z XML rozvržení.
     *
     * @param context Kontext aplikace
     * @param attrs   Atributy XML
     */
    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }


    /**
     * Vytvoří instanci {@link ZoomableImageView} z XML rozvržení s výchozím stylem.
     *
     * @param context      Kontext aplikace
     * @param attrs        Atributy XML
     * @param defStyleAttr Výchozí atribut stylu
     */
    public ZoomableImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setScaleType(ScaleType.MATRIX);

        documentBackgroundPaint.setColor(Color.WHITE);
        documentBackgroundPaint.setStyle(Paint.Style.FILL);

        flingScroller = new OverScroller(context);
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
        scaleGestureDetector.setQuickScaleEnabled(false);
        gestureDetector = new GestureDetector(context, new GestureListener());
    }


    /**
     * Nastaví posluchače změn úrovně přiblížení.
     *
     * @param listener Posluchač změny zvětšení nebo {@code null}
     */
    public void setOnZoomChangeListener(@Nullable OnZoomChangeListener listener) {
        this.onZoomChangeListener = listener;
    }


    /**
     * Nastaví posluchače jednoduchého klepnutí na plochu prohlížeče.
     *
     * @param listener Posluchač klepnutí nebo {@code null}
     */
    public void setOnSingleTapListener(@Nullable OnSingleTapListener listener) {
        this.onSingleTapListener = listener;
    }


    /**
     * Nastaví bezpečné vnitřní okraje pro výchozí přizpůsobení obrázku (např. výška horní a spodní lišty),
     * aby úvodní náhled nebyl překryt ovládacími prvky, zatímco při přiblížení lze posouvat po celé ploše.
     *
     * @param left   Levý okraj v pixelech
     * @param top    Horní okraj v pixelech
     * @param right  Pravý okraj v pixelech
     * @param bottom Spodní okraj v pixelech
     */
    public void setFitInsets(int left, int top, int right, int bottom) {
        if (this.insetLeft == left
                && this.insetTop == top
                && this.insetRight == right
                && this.insetBottom == bottom) {
            return;
        }
        this.insetLeft = left;
        this.insetTop = top;
        this.insetRight = right;
        this.insetBottom = bottom;
        if (currentZoom <= MIN_ZOOM + 0.05f) {
            fitImageToView();
        }
    }


    /**
     * Plynule vrátí zobrazení na výchozí přiblížení (1×), kdy je vidět celý dokument.
     */
    public void resetZoom() {
        if (getDrawable() == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        animateZoomTo(MIN_ZOOM, getWidth() / 2f, getHeight() / 2f);
    }


    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        super.setImageDrawable(drawable);
        cancelAnimations();
        fitImageToView();
    }


    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            cancelAnimations();
            fitImageToView();
        }
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable != null && drawable.getIntrinsicWidth() > 0 && drawable.getIntrinsicHeight() > 0) {
            mappedBounds.set(0f, 0f, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            getImageMatrix().mapRect(mappedBounds);
            canvas.drawRect(mappedBounds, documentBackgroundPaint);
        }
        super.onDraw(canvas);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (getDrawable() == null) {
            return super.onTouchEvent(event);
        }

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            stopFling();
        }

        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);

        if (event.getActionMasked() == MotionEvent.ACTION_UP && !scaleGestureDetector.isInProgress()) {
            performClick();
        }

        return scaleHandled || gestureHandled || super.onTouchEvent(event);
    }


    @Override
    public boolean performClick() {
        return super.performClick();
    }


    @Override
    protected void onDetachedFromWindow() {
        cancelAnimations();
        super.onDetachedFromWindow();
    }


    /**
     * Přizpůsobí obrázek rozměrům pohledu při zachování původního poměru stran a vycentruje jej.
     */
    private void fitImageToView() {
        Drawable drawable = getDrawable();
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (drawable == null || viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        int imgWidth = drawable.getIntrinsicWidth();
        int imgHeight = drawable.getIntrinsicHeight();
        if (imgWidth <= 0 || imgHeight <= 0) {
            return;
        }

        float availableWidth = Math.max(1f, viewWidth - insetLeft - insetRight);
        float availableHeight = Math.max(1f, viewHeight - insetTop - insetBottom);

        baseScale = Math.min(availableWidth / imgWidth, availableHeight / imgHeight);
        currentZoom = MIN_ZOOM;

        float scaledWidth = imgWidth * baseScale;
        float scaledHeight = imgHeight * baseScale;
        float translateX = insetLeft + (availableWidth - scaledWidth) / 2f;
        float translateY = insetTop + (availableHeight - scaledHeight) / 2f;

        drawMatrix.reset();
        drawMatrix.postScale(baseScale, baseScale);
        drawMatrix.postTranslate(translateX, translateY);
        setImageMatrix(drawMatrix);
        notifyZoomChanged();
    }


    /**
     * Omezí posun matice tak, aby se obrázek neposunul mimo viditelnou oblast pohledu.
     */
    private void constrainTranslation() {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }
        int imgWidth = drawable.getIntrinsicWidth();
        int imgHeight = drawable.getIntrinsicHeight();
        if (imgWidth <= 0 || imgHeight <= 0) {
            return;
        }

        mappedBounds.set(0f, 0f, imgWidth, imgHeight);
        drawMatrix.mapRect(mappedBounds);

        float viewWidth = getWidth();
        float viewHeight = getHeight();
        float availWidth = Math.max(1f, viewWidth - insetLeft - insetRight);
        float availHeight = Math.max(1f, viewHeight - insetTop - insetBottom);

        float deltaX = 0f;
        if (mappedBounds.width() <= viewWidth) {
            float targetLeft = insetLeft + (availWidth - mappedBounds.width()) / 2f;
            if (targetLeft < 0f || targetLeft + mappedBounds.width() > viewWidth) {
                targetLeft = (viewWidth - mappedBounds.width()) / 2f;
            }
            deltaX = targetLeft - mappedBounds.left;
        } else if (mappedBounds.left > 0f) {
            deltaX = -mappedBounds.left;
        } else if (mappedBounds.right < viewWidth) {
            deltaX = viewWidth - mappedBounds.right;
        }

        float deltaY = 0f;
        if (mappedBounds.height() <= viewHeight) {
            float targetTop = insetTop + (availHeight - mappedBounds.height()) / 2f;
            if (targetTop < 0f || targetTop + mappedBounds.height() > viewHeight) {
                targetTop = (viewHeight - mappedBounds.height()) / 2f;
            }
            deltaY = targetTop - mappedBounds.top;
        } else if (mappedBounds.top > 0f) {
            deltaY = -mappedBounds.top;
        } else if (mappedBounds.bottom < viewHeight) {
            deltaY = viewHeight - mappedBounds.bottom;
        }

        if (deltaX != 0f || deltaY != 0f) {
            drawMatrix.postTranslate(deltaX, deltaY);
        }
    }


    /**
     * Plynule animuje přiblížení na cílovou hodnotu kolem zvoleného bodu.
     *
     * @param targetZoom Cílový faktor zvětšení
     * @param focusX     X souřadnice ohniska v pohledu
     * @param focusY     Y souřadnice ohniska v pohledu
     */
    private void animateZoomTo(float targetZoom, float focusX, float focusY) {
        cancelAnimations();
        float clampedTarget = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, targetZoom));
        float startZoom = currentZoom;

        zoomAnimator = ValueAnimator.ofFloat(startZoom, clampedTarget);
        zoomAnimator.setDuration(ZOOM_ANIMATION_DURATION_MS);
        zoomAnimator.setInterpolator(new DecelerateInterpolator());
        zoomAnimator.addUpdateListener(animation -> {
            float animatedZoom = (float) animation.getAnimatedValue();
            float currentScale = getCurrentMatrixScale();
            if (currentScale <= 0f) {
                return;
            }
            float targetScale = baseScale * animatedZoom;
            float scaleFactor = targetScale / currentScale;
            drawMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY);
            currentZoom = animatedZoom;
            constrainTranslation();
            setImageMatrix(drawMatrix);
            notifyZoomChanged();
        });
        zoomAnimator.start();
    }


    private float getCurrentMatrixScale() {
        drawMatrix.getValues(matrixValues);
        return matrixValues[Matrix.MSCALE_X];
    }


    private void stopFling() {
        if (isFlinging) {
            isFlinging = false;
            flingScroller.forceFinished(true);
            removeCallbacks(flingRunnable);
        }
    }


    private void cancelAnimations() {
        stopFling();
        if (zoomAnimator != null) {
            zoomAnimator.cancel();
            zoomAnimator = null;
        }
    }


    private void notifyZoomChanged() {
        if (onZoomChangeListener != null) {
            onZoomChangeListener.onZoomChanged(currentZoom);
        }
    }


    private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(@NonNull ScaleGestureDetector detector) {
            cancelAnimations();
            return true;
        }


        @Override
        public boolean onScale(@NonNull ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor) || scaleFactor <= 0f) {
                return false;
            }

            float targetZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, currentZoom * scaleFactor));
            float appliedFactor = targetZoom / currentZoom;
            if (Math.abs(appliedFactor - 1.0f) < 0.0001f) {
                return true;
            }

            currentZoom = targetZoom;
            drawMatrix.postScale(appliedFactor, appliedFactor, detector.getFocusX(), detector.getFocusY());
            constrainTranslation();
            setImageMatrix(drawMatrix);
            notifyZoomChanged();
            return true;
        }

    }


    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }


        @Override
        public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
            if (onSingleTapListener != null) {
                onSingleTapListener.onSingleTap();
                return true;
            }
            return false;
        }


        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            if (currentZoom > MIN_ZOOM + 0.1f) {
                animateZoomTo(MIN_ZOOM, e.getX(), e.getY());
            } else {
                animateZoomTo(DOUBLE_TAP_ZOOM, e.getX(), e.getY());
            }
            return true;
        }


        @Override
        public boolean onScroll(
                @Nullable MotionEvent e1,
                @NonNull MotionEvent e2,
                float distanceX,
                float distanceY
        ) {
            if (scaleGestureDetector.isInProgress() || currentZoom <= MIN_ZOOM + 0.01f) {
                return false;
            }
            drawMatrix.postTranslate(-distanceX, -distanceY);
            constrainTranslation();
            setImageMatrix(drawMatrix);
            return true;
        }


        @Override
        public boolean onFling(
                @Nullable MotionEvent e1,
                @NonNull MotionEvent e2,
                float velocityX,
                float velocityY
        ) {
            if (scaleGestureDetector.isInProgress() || currentZoom <= MIN_ZOOM + 0.01f) {
                return false;
            }
            Drawable drawable = getDrawable();
            if (drawable == null) {
                return false;
            }

            mappedBounds.set(0f, 0f, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawMatrix.mapRect(mappedBounds);

            int maxOffsetX = Math.max(0, Math.round(mappedBounds.width() - getWidth()));
            int maxOffsetY = Math.max(0, Math.round(mappedBounds.height() - getHeight()));
            if (maxOffsetX == 0 && maxOffsetY == 0) {
                return false;
            }

            lastFlingX = Math.round(mappedBounds.left);
            lastFlingY = Math.round(mappedBounds.top);
            flingScroller.forceFinished(true);
            flingScroller.fling(
                    lastFlingX,
                    lastFlingY,
                    Math.round(velocityX),
                    Math.round(velocityY),
                    -maxOffsetX,
                    0,
                    -maxOffsetY,
                    0
            );
            isFlinging = true;
            postOnAnimation(flingRunnable);
            return true;
        }

    }

}
