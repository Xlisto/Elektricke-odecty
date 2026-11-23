package cz.xlisto.elektrodroid.modules.dashboard;


import static cz.xlisto.elektrodroid.utils.DensityUtils.dpToPx;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.HdoModel;
import cz.xlisto.elektrodroid.utils.BuilderHDOStack;


/**
 * Kompaktní 24hodinové analogové hodiny zobrazující dnešní intervaly HDO
 * rozdělené do 3 druhů spínaných časů (TUV, TAR, PV) ve třech neutrálních barvách
 * s plynulou animací posunu začátku a konce výsečí při přepínání odběrných míst.
 */
public class DashboardHdoClockView extends View {

    private static final int[] HOURS_8 = {0, 3, 6, 9, 12, 15, 18, 21};
    private static final int[] HOURS_4 = {0, 6, 12, 18};
    private static final long ANIMATION_DURATION_MS = 550L;
    private static final float RING_STEP_RATIO = 0.28f;


    /**
     * Druhy spínaných relé HDO podle původního přehledu:
     * TUV (bojler/akumulace), TAR (tarif/přímotop), PV (fotovoltaika).
     */
    private enum RelayType {
        TUV,
        TAR,
        PV
    }


    /**
     * Stav jedné výseče HDO na ciferníku (úhel začátku, délka výseče, relativní poloměr a barva).
     */
    private static class SectorState {

        final RelayType type;
        final float startAngle;
        final float sweepAngle;
        final float radiusRatio;
        final int color;


        SectorState(@NonNull RelayType type, float startAngle, float sweepAngle, float radiusRatio, int color) {
            this.type = type;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
            this.radiusRatio = radiusRatio;
            this.color = color;
        }

    }


    /**
     * Dvojice počátečního a cílového stavu výseče pro plynulou interpolaci (posun začátku/konce/poloměru).
     */
    private static class SectorTransition {

        final SectorState from;
        final SectorState to;


        SectorTransition(@NonNull SectorState from, @NonNull SectorState to) {
            this.from = from;
            this.to = to;
        }


        SectorState interpolate(float fraction, @NonNull ArgbEvaluator colorEvaluator) {
            float start = from.startAngle + (to.startAngle - from.startAngle) * fraction;
            float sweep = from.sweepAngle + (to.sweepAngle - from.sweepAngle) * fraction;
            float ratio = from.radiusRatio + (to.radiusRatio - from.radiusRatio) * fraction;
            int color = (from.color == to.color)
                    ? to.color
                    : (int) colorEvaluator.evaluate(fraction, from.color, to.color);
            return new SectorState(to.type, start, sweep, ratio, color);
        }

    }

    private Paint paintDialBg;
    private Paint paintDialStroke;
    private Paint paintSector;
    private Paint paintTick;
    private Paint paintNumbers;
    private Paint paintHand;
    private Paint paintCenterDot;

    private int colorHdoTuv;
    private int colorHdoTar;
    private int colorHdoPv;

    private final RectF arcRect = new RectF();
    private final Calendar todayCal = Calendar.getInstance();
    private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

    private List<HdoModel> rawModels = new ArrayList<>();
    private List<HdoModel> weekModels = new ArrayList<>();
    private long timeShift = 0L;

    private List<SectorState> currentDisplayedSectors = new ArrayList<>();
    private List<SectorTransition> activeTransitions = new ArrayList<>();
    private float animFraction = 1f;

    private long handAnimatedTimeMillis = -1L;
    private long lastDisplayedTimeMillis = -1L;

    private ValueAnimator morphAnimator;
    private ValueAnimator handAnimator;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            invalidate();
            handler.postDelayed(this, 30_000L);
        }
    };


    public DashboardHdoClockView(Context context) {
        super(context);
        init();
    }


    public DashboardHdoClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }


    public DashboardHdoClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private void init() {
        int colorPrimary = ContextCompat.getColor(getContext(), R.color.dashboard_teal_primary);
        int colorOnSurface = resolveAttrColor(com.google.android.material.R.attr.colorOnSurface, 0xFF1C1B1F);
        int colorSurfaceBg = ContextCompat.getColor(getContext(), R.color.dashboard_price_card_start);

        colorHdoTuv = ContextCompat.getColor(getContext(), R.color.color_hdo_time_TUV);
        colorHdoTar = ContextCompat.getColor(getContext(), R.color.color_hdo_time_TAR);
        colorHdoPv = ContextCompat.getColor(getContext(), R.color.color_hdo_time_PV);

        paintDialBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDialBg.setStyle(Paint.Style.FILL);
        paintDialBg.setColor(colorSurfaceBg);

        paintDialStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintDialStroke.setStyle(Paint.Style.STROKE);
        paintDialStroke.setStrokeWidth(dpToPx(getContext(), 1.5f));
        paintDialStroke.setColor(colorPrimary);

        paintSector = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintSector.setStyle(Paint.Style.FILL);

        paintTick = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintTick.setStyle(Paint.Style.STROKE);
        paintTick.setStrokeWidth(dpToPx(getContext(), 1f));
        paintTick.setColor(colorOnSurface);
        paintTick.setAlpha(150);

        paintNumbers = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNumbers.setStyle(Paint.Style.FILL);
        paintNumbers.setColor(colorOnSurface);
        paintNumbers.setTextAlign(Paint.Align.CENTER);
        paintNumbers.setTextSize(dpToPx(getContext(), 7.5f));
        paintNumbers.setFakeBoldText(true);

        paintHand = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHand.setStyle(Paint.Style.STROKE);
        paintHand.setStrokeWidth(dpToPx(getContext(), 1.8f));
        paintHand.setStrokeCap(Paint.Cap.ROUND);
        paintHand.setColor(0xFFD32F2F);

        paintCenterDot = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCenterDot.setStyle(Paint.Style.FILL);
        paintCenterDot.setColor(0xFFD32F2F);
    }


    private int resolveAttrColor(int attrRes, int fallback) {
        TypedValue tv = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrRes, tv, true)) {
            return tv.data;
        }
        return fallback;
    }


    /**
     * Nastaví seznam HDO modelů a časový posun elektroměru (s animací přechodu).
     */
    public void setHdoData(@Nullable List<HdoModel> models, long timeShiftMillis) {
        setHdoData(models, timeShiftMillis, true);
    }


    /**
     * Nastaví seznam HDO modelů a časový posun elektroměru s možností volby animace.
     *
     * @param models          seznam HDO záznamů odběrného místa
     * @param timeShiftMillis posun času elektroměru v ms
     * @param animate         true pro plynulý posun výsečí a ručičky (při přepnutí odběrného místa)
     */
    public void setHdoData(@Nullable List<HdoModel> models, long timeShiftMillis, boolean animate) {
        // Pokud probíhala předchozí animace, zachytíme aktuální mezistav výsečí jako nový výchozí bod
        List<SectorState> previousSectors = getInterpolatedSectors();

        this.timeShift = timeShiftMillis;
        this.rawModels = models != null ? new ArrayList<>(models) : new ArrayList<>();
        rebuildWeekModels();

        long targetTimeMillis = System.currentTimeMillis() + timeShiftMillis;
        todayCal.setTimeInMillis(targetTimeMillis);
        List<SectorState> targetSectors = buildTodaySectors(todayCal);

        if (!animate || !isAttachedToWindow()) {
            cancelAnimations();
            currentDisplayedSectors = targetSectors;
            activeTransitions.clear();
            animFraction = 1f;
            handAnimatedTimeMillis = targetTimeMillis;
            lastDisplayedTimeMillis = targetTimeMillis;
            invalidate();
            return;
        }

        long startTimeMillis;
        if (lastDisplayedTimeMillis > 0) {
            startTimeMillis = lastDisplayedTimeMillis;
        } else {
            Calendar midnight = Calendar.getInstance();
            midnight.setTimeInMillis(targetTimeMillis);
            midnight.set(Calendar.HOUR_OF_DAY, 0);
            midnight.set(Calendar.MINUTE, 0);
            midnight.set(Calendar.SECOND, 0);
            midnight.set(Calendar.MILLISECOND, 0);
            startTimeMillis = midnight.getTimeInMillis();
        }
        lastDisplayedTimeMillis = targetTimeMillis;

        startMorphAnimation(previousSectors, targetSectors, startTimeMillis, targetTimeMillis);
    }


    private void rebuildWeekModels() {
        if (!rawModels.isEmpty()) {
            this.weekModels = BuilderHDOStack.build(new ArrayList<>(rawModels), timeShift);
        } else {
            this.weekModels = new ArrayList<>();
        }
    }


    /**
     * Sestaví seznam dnešních výsečí HDO seřazených podle druhu relé (TUV, TAR, PV) a času začátku.
     */
    @NonNull
    private List<SectorState> buildTodaySectors(@NonNull Calendar dayCal) {
        if (weekModels == null || weekModels.isEmpty()) {
            return new ArrayList<>();
        }

        Map<RelayType, List<HdoModel>> byType = new EnumMap<>(RelayType.class);
        for (RelayType type : RelayType.values()) {
            byType.put(type, new ArrayList<>());
        }

        for (HdoModel model : weekModels) {
            if (isSameDay(model.getCalendarStart(), dayCal)) {
                RelayType type = classifyRelayType(model.getRele());
                List<HdoModel> list = byType.get(type);
                if (list != null) {
                    list.add(model);
                }
            }
        }

        List<SectorState> result = new ArrayList<>();
        int activeTypesCount = 0;

        for (RelayType type : RelayType.values()) {
            List<HdoModel> modelsForType = byType.get(type);
            if (modelsForType == null || modelsForType.isEmpty()) {
                continue;
            }
            float radiusRatio = Math.max(0.2f, 1.0f - (activeTypesCount * RING_STEP_RATIO));
            activeTypesCount++;
            int color = getColorForRelayType(type);

            List<SectorState> typeSectors = new ArrayList<>();
            for (HdoModel model : modelsForType) {
                float startAngle = convertTimeToAngle(model.getTimeFrom());
                float endAngle = convertTimeToAngle(model.getTimeUntil());
                float rawSweep = endAngle - startAngle;
                float sweepAngle = rawSweep <= 0f ? rawSweep + 360f : rawSweep;
                typeSectors.add(new SectorState(type, startAngle, sweepAngle, radiusRatio, color));
            }
            typeSectors.sort((a, b) -> Float.compare(a.startAngle, b.startAngle));
            result.addAll(typeSectors);
        }

        return result;
    }


    /**
     * Spáruje předchozí a nové výseče tak, aby se při změně odběrného místa plynule posunuly
     * jejich začátky, konce i poloměry mezikruží.
     */
    @NonNull
    private List<SectorTransition> buildTransitions(@NonNull List<SectorState> oldSectors,
                                                    @NonNull List<SectorState> newSectors) {
        Map<RelayType, List<SectorState>> oldByType = groupByType(oldSectors);
        Map<RelayType, List<SectorState>> newByType = groupByType(newSectors);

        List<SectorTransition> transitions = new ArrayList<>();
        List<RelayType> unmatchedOldTypes = new ArrayList<>();
        List<RelayType> unmatchedNewTypes = new ArrayList<>();

        // 1. Spárování shodných druhů relé (TUV -> TUV, TAR -> TAR, PV -> PV)
        for (RelayType type : RelayType.values()) {
            List<SectorState> oList = oldByType.get(type);
            List<SectorState> nList = newByType.get(type);
            boolean hasOld = oList != null && !oList.isEmpty();
            boolean hasNew = nList != null && !nList.isEmpty();

            if (hasOld && hasNew) {
                pairSectorLists(oList, nList, transitions);
            } else if (hasOld) {
                unmatchedOldTypes.add(type);
            } else if (hasNew) {
                unmatchedNewTypes.add(type);
            }
        }

        // 2. Pokud staré místo mělo jiný druh relé než nové místo (např. TUV vs TAR),
        // spárujeme je mezi sebou, aby se výseče plynule posunuly po ciferníku
        int pairCount = Math.min(unmatchedOldTypes.size(), unmatchedNewTypes.size());
        for (int i = 0; i < pairCount; i++) {
            List<SectorState> oList = oldByType.get(unmatchedOldTypes.get(i));
            List<SectorState> nList = newByType.get(unmatchedNewTypes.get(i));
            if (oList != null && nList != null) {
                pairSectorLists(oList, nList, transitions);
            }
        }

        // 3. Zbývající nespárované staré druhy relé se plynule uzavřou (začátek doběhne konec)
        for (int i = pairCount; i < unmatchedOldTypes.size(); i++) {
            List<SectorState> oList = oldByType.get(unmatchedOldTypes.get(i));
            if (oList != null) {
                pairSectorLists(oList, Collections.emptyList(), transitions);
            }
        }

        // 4. Zbývající nespárované nové druhy relé se plynule rozevřou od svého začátku ke konci
        for (int i = pairCount; i < unmatchedNewTypes.size(); i++) {
            List<SectorState> nList = newByType.get(unmatchedNewTypes.get(i));
            if (nList != null) {
                pairSectorLists(Collections.emptyList(), nList, transitions);
            }
        }

        return transitions;
    }


    private void pairSectorLists(@NonNull List<SectorState> oldList,
                                 @NonNull List<SectorState> newList,
                                 @NonNull List<SectorTransition> outTransitions) {
        int m = oldList.size();
        int n = newList.size();
        int max = Math.max(m, n);

        for (int i = 0; i < max; i++) {
            if (i < m && i < n) {
                // Přímý posun začátku, délky a poloměru i-té výseče
                outTransitions.add(new SectorTransition(oldList.get(i), newList.get(i)));
            } else if (i >= m) {
                // Nová výseč navíc: plynule se vykreslí (rozevře) od svého začátku ke konci
                SectorState target = newList.get(i);
                SectorState startEmpty = new SectorState(
                        target.type,
                        target.startAngle,
                        0f,
                        target.radiusRatio,
                        target.color
                );
                outTransitions.add(new SectorTransition(startEmpty, target));
            } else {
                // Stará výseč navíc: plynule se uzavře po směru hodinových ručiček
                SectorState source = oldList.get(i);
                SectorState endEmpty = new SectorState(
                        source.type,
                        source.startAngle + source.sweepAngle,
                        0f,
                        source.radiusRatio,
                        source.color
                );
                outTransitions.add(new SectorTransition(source, endEmpty));
            }
        }
    }


    @NonNull
    private Map<RelayType, List<SectorState>> groupByType(@NonNull List<SectorState> sectors) {
        Map<RelayType, List<SectorState>> map = new EnumMap<>(RelayType.class);
        for (RelayType type : RelayType.values()) {
            map.put(type, new ArrayList<>());
        }
        for (SectorState s : sectors) {
            List<SectorState> list = map.get(s.type);
            if (list != null) {
                list.add(s);
            }
        }
        return map;
    }


    private void startMorphAnimation(@NonNull List<SectorState> fromSectors,
                                     @NonNull List<SectorState> toSectors,
                                     long fromTimeMillis,
                                     long toTimeMillis) {
        cancelAnimations();

        // 1. Animace ručičky
        if (Math.abs(toTimeMillis - fromTimeMillis) > 1000L) {
            handAnimator = ValueAnimator.ofFloat(0f, 1f);
            handAnimator.setDuration(ANIMATION_DURATION_MS);
            handAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
            handAnimator.addUpdateListener(anim -> {
                float fraction = (float) anim.getAnimatedValue();
                handAnimatedTimeMillis = (long) (fromTimeMillis + (toTimeMillis - fromTimeMillis) * fraction);
                invalidate();
            });
            handAnimator.start();
        } else {
            handAnimatedTimeMillis = toTimeMillis;
        }

        // 2. Animace postupného posunu / překreslení výsečí HDO
        currentDisplayedSectors = toSectors;
        activeTransitions = buildTransitions(fromSectors, toSectors);
        if (activeTransitions.isEmpty()) {
            animFraction = 1f;
            invalidate();
            return;
        }

        animFraction = 0f;
        morphAnimator = ValueAnimator.ofFloat(0f, 1f);
        morphAnimator.setDuration(ANIMATION_DURATION_MS);
        morphAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        morphAnimator.addUpdateListener(anim -> {
            animFraction = (float) anim.getAnimatedValue();
            invalidate();
        });
        morphAnimator.start();
    }


    @NonNull
    private List<SectorState> getInterpolatedSectors() {
        if (morphAnimator == null || !morphAnimator.isRunning() || activeTransitions.isEmpty()) {
            return new ArrayList<>(currentDisplayedSectors);
        }
        List<SectorState> list = new ArrayList<>(activeTransitions.size());
        for (SectorTransition transition : activeTransitions) {
            SectorState s = transition.interpolate(animFraction, argbEvaluator);
            if (s.sweepAngle > 0.1f) {
                list.add(s);
            }
        }
        // Seřadit od největšího poloměru (vnější kruh) k nejmenšímu (vnitřní kruh)
        list.sort((a, b) -> Float.compare(b.radiusRatio, a.radiusRatio));
        return list;
    }


    private int getColorForRelayType(@NonNull RelayType type) {
        if (type == RelayType.TUV) {
            return colorHdoTuv;
        }
        if (type == RelayType.PV) {
            return colorHdoPv;
        }
        return colorHdoTar;
    }


    private void cancelAnimations() {
        if (morphAnimator != null) {
            morphAnimator.cancel();
            morphAnimator = null;
        }
        if (handAnimator != null) {
            handAnimator.cancel();
            handAnimator = null;
        }
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.removeCallbacks(ticker);
        handler.postDelayed(ticker, 30_000L);
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeCallbacks(ticker);
        cancelAnimations();
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        if (size <= 0) {
            return;
        }

        float cx = w / 2f;
        float cy = h / 2f;
        float pad = dpToPx(getContext(), 2f);
        float radius = (size / 2f) - pad;

        // 1. Pozadí ciferníku
        canvas.drawCircle(cx, cy, radius, paintDialBg);

        // 2. Výseče 24h HDO pro dnešní den (s plynulým posunem začátků a konců při animaci)
        List<SectorState> sectorsToDraw = getInterpolatedSectors();
        for (SectorState sector : sectorsToDraw) {
            if (sector.sweepAngle <= 0.05f) {
                continue;
            }
            float sectorRadius = radius * sector.radiusRatio;
            if (sectorRadius <= 0f) {
                continue;
            }
            paintSector.setColor(sector.color);
            arcRect.set(cx - sectorRadius, cy - sectorRadius, cx + sectorRadius, cy + sectorRadius);
            canvas.drawArc(arcRect, sector.startAngle, sector.sweepAngle, true, paintSector);
        }

        // 3. Značky po 1 hodině (24 dílků)
        float majorTickLen = dpToPx(getContext(), 3.5f);
        float minorTickLen = dpToPx(getContext(), 2f);
        for (int i = 0; i < 24; i++) {
            double rad = Math.toRadians(270 + i * 15);
            float tickLen = (i % 3 == 0) ? majorTickLen : minorTickLen;
            float sx = (float) (cx + radius * Math.cos(rad));
            float sy = (float) (cy + radius * Math.sin(rad));
            float ex = (float) (cx + (radius - tickLen) * Math.cos(rad));
            float ey = (float) (cy + (radius - tickLen) * Math.sin(rad));
            canvas.drawLine(sx, sy, ex, ey, paintTick);
        }

        // 4. Čísla ciferníku (0, 6, 12, 18 nebo násobky 3 podle velikosti)
        int[] hoursToShow = (radius >= dpToPx(getContext(), 26)) ? HOURS_8 : HOURS_4;
        float textRadius = radius - majorTickLen - dpToPx(getContext(), 5.5f);
        Paint.FontMetrics fm = paintNumbers.getFontMetrics();
        float textVerticalOffset = -(fm.ascent + fm.descent) / 2f;

        for (int hour : hoursToShow) {
            double rad = Math.toRadians(270 + hour * 15);
            float tx = (float) (cx + textRadius * Math.cos(rad));
            float ty = (float) (cy + textRadius * Math.sin(rad)) + textVerticalOffset;
            canvas.drawText(String.valueOf(hour), tx, ty, paintNumbers);
        }

        // 5. Obrys ciferníku
        canvas.drawCircle(cx, cy, radius, paintDialStroke);

        // 6. Ručička aktuálního času (24h režim, podporuje plynulou animaci)
        long currentTargetMillis = System.currentTimeMillis() + timeShift;
        long handMillis = (handAnimator != null && handAnimator.isRunning() && handAnimatedTimeMillis > 0)
                ? handAnimatedTimeMillis
                : currentTargetMillis;
        lastDisplayedTimeMillis = handMillis;
        todayCal.setTimeInMillis(handMillis);

        int hours = todayCal.get(Calendar.HOUR_OF_DAY);
        int minutes = todayCal.get(Calendar.MINUTE);
        int seconds = todayCal.get(Calendar.SECOND);
        double handAngleRad = Math.toRadians(270 + hours * 15 + minutes * 0.25 + seconds * (0.25 / 60.0));
        float handLen = radius - dpToPx(getContext(), 5f);
        float hx = (float) (cx + handLen * Math.cos(handAngleRad));
        float hy = (float) (cy + handLen * Math.sin(handAngleRad));
        canvas.drawLine(cx, cy, hx, hy, paintHand);
        canvas.drawCircle(cx, cy, dpToPx(getContext(), 2.2f), paintCenterDot);
    }


    /**
     * Rozpozná druh spínaného relé HDO podle názvu relé (shodně s původním {@link GraphTotalHdoView}).
     */
    @NonNull
    private RelayType classifyRelayType(@Nullable String rele) {
        if (rele == null || rele.trim().isEmpty()) {
            return RelayType.TAR;
        }
        String r = rele.toUpperCase(Locale.ROOT);
        boolean isTuv = r.contains("TUV") || r.contains("AKU") || r.contains("BOJLER");
        boolean isPv = !isTuv && (r.contains("PV") || r.contains("FVE") || r.contains("FOTOVOLT"));
        if (isPv) {
            return RelayType.PV;
        }
        if (isTuv) {
            return RelayType.TUV;
        }
        return RelayType.TAR;
    }


    private boolean isSameDay(@Nullable Calendar c1, @Nullable Calendar c2) {
        if (c1 == null || c2 == null) {
            return false;
        }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }


    private float convertTimeToAngle(@Nullable String time) {
        if (time == null || !time.contains(":")) {
            return 270f;
        }
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0].trim());
            int minutes = Integer.parseInt(parts[1].trim());
            return 270f + (hours * 15f) + (minutes * 0.25f);
        } catch (NumberFormatException e) {
            return 270f;
        }
    }

}
