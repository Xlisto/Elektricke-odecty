package cz.xlisto.elektrodroid.modules.hdo;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.HdoModel;

/**
 * Zvětšené 24hodinové grafické zobrazení ciferníku hodin pro konkrétní skupinu HDO časů.
 * Xlisto 30.01.2026
 */
public class GraphHdoClockView extends View {

    private final ArrayList<HdoModel> models = new ArrayList<>();

    private Paint pClock;
    private Paint pNumbers;
    private Paint pTimeTUV;
    private Paint pTimeTAR;
    private Paint pTimePV;
    private Paint pLegend;
    private Paint pTitle;
    private Paint pTick;
    private Paint pBackground;

    private boolean isDarkMode;
    private String groupTitle = "";
    private int radius;
    private int centerX;
    private int centerY;

    private boolean showTUV;
    private boolean showTAR;
    private boolean showPV;
    private int activeTypesCount = 0;
    private int tuvOffset;
    private int tarOffset;
    private int pvOffset;
    private int smallerUnit;

    private boolean showClockHand = false;
    private int currentMeterMinutes = -1;
    private Drawable bellDrawable;

    private enum RelayType {
        TUV, TAR, PV, UNKNOWN
    }

    public GraphHdoClockView(Context context) {
        super(context);
        init();
    }

    public GraphHdoClockView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public GraphHdoClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        int primaryTextColor = Color.BLACK;
        try {
            TypedValue typedValue = new TypedValue();
            if (getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)) {
                primaryTextColor = typedValue.resourceId != 0
                        ? ContextCompat.getColor(getContext(), typedValue.resourceId)
                        : typedValue.data;
            }
        } catch (Exception ignored) {
        }

        pClock = new Paint(Paint.ANTI_ALIAS_FLAG);
        pClock.setColor(primaryTextColor);
        pClock.setStyle(Paint.Style.STROKE);
        pClock.setStrokeWidth(dpToPx(1.5f));

        pNumbers = new Paint(Paint.ANTI_ALIAS_FLAG);
        pNumbers.setColor(primaryTextColor);
        pNumbers.setStyle(Paint.Style.FILL);
        pNumbers.setTextSize(dpToPx(11f));
        pNumbers.setTextAlign(Paint.Align.CENTER);

        pLegend = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLegend.setColor(primaryTextColor);
        pLegend.setStyle(Paint.Style.FILL);
        pLegend.setTextSize(dpToPx(12f));
        pLegend.setTextAlign(Paint.Align.LEFT);

        pTitle = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTitle.setColor(primaryTextColor);
        pTitle.setStyle(Paint.Style.FILL);
        pTitle.setTextSize(dpToPx(14f));
        pTitle.setFakeBoldText(true);
        pTitle.setTextAlign(Paint.Align.LEFT);

        pTick = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTick.setColor(Color.RED);
        pTick.setStyle(Paint.Style.FILL_AND_STROKE);
        pTick.setStrokeWidth(dpToPx(2f));
        pTick.setStrokeCap(Paint.Cap.ROUND);

        pTimeTUV = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTimeTUV.setColor(ContextCompat.getColor(getContext(), R.color.color_hdo_time_TUV));
        pTimeTUV.setStyle(Paint.Style.FILL);

        pTimeTAR = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTimeTAR.setColor(ContextCompat.getColor(getContext(), R.color.color_hdo_time_TAR));
        pTimeTAR.setStyle(Paint.Style.FILL);

        pTimePV = new Paint(Paint.ANTI_ALIAS_FLAG);
        pTimePV.setColor(ContextCompat.getColor(getContext(), R.color.color_hdo_time_PV));
        pTimePV.setStyle(Paint.Style.FILL);

        isDarkMode = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        pBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBackground.setStyle(Paint.Style.FILL);
        pBackground.setColor(Color.BLACK);

        bellDrawable = ContextCompat.getDrawable(getContext(), R.drawable.ic_bell_24);
        if (bellDrawable != null) {
            bellDrawable.setTint(primaryTextColor);
        }
    }


    /**
     * Nastaví název skupiny (např. Všední dny, Víkend, Svátek)
     *
     * @param title název skupiny
     */
    public void setGroupTitle(String title) {
        this.groupTitle = title != null ? title : "";
        invalidate();
    }


    /**
     * Nastaví seznam HDO modelů pro vykreslení
     *
     * @param newModels seznam HDO modelů
     */
    public void setModels(List<HdoModel> newModels) {
        this.models.clear();
        if (newModels != null) {
            this.models.addAll(newModels);
        }
        updateActiveTypesCount();
        requestLayout();
        invalidate();
    }


    /**
     * Přepočítá počet aktivních typů relé (TUV, TAR, PV)
     */
    private void updateActiveTypesCount() {
        showTUV = false;
        showTAR = false;
        showPV = false;
        for (HdoModel model : models) {
            RelayType type = getRelayType(model.getRele());
            if (type == RelayType.TUV) showTUV = true;
            else if (type == RelayType.TAR) showTAR = true;
            else if (type == RelayType.PV) showPV = true;
            else showTAR = true;
        }
        activeTypesCount = (showTUV ? 1 : 0) + (showTAR ? 1 : 0) + (showPV ? 1 : 0);
    }


    /**
     * Nastaví zobrazení hodinové ručičky a minutový čas elektroměru
     *
     * @param show    {@code true} pro zobrazení ručičky, {@code false} pro skrytí
     * @param minutes aktuální čas elektroměru v minutách od půlnoci
     */
    public void setClockHand(boolean show, int minutes) {
        this.showClockHand = show;
        this.currentMeterMinutes = minutes;
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int targetHeight = (activeTypesCount == 1) ? dpToPx(175f) : dpToPx(220f);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.AT_MOST) {
            height = targetHeight;
        } else if (activeTypesCount == 1) {
            height = targetHeight;
        }
        setMeasuredDimension(width, height);
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int availableHeight = getHeight();
        int availableWidth = getWidth();
        if (availableHeight <= 0 || availableWidth <= 0) return;

        int padding = dpToPx(4);

        if (activeTypesCount == 1) {
            // Režim 1 relé: Ciferník vystředěný v buňce (název je v horní liště karty v XML)
            int dialDiameter = Math.min(availableHeight - padding * 2, (int) (availableWidth * 0.85f));
            radius = dialDiameter / 2;
            centerX = availableWidth / 2;
            centerY = availableHeight / 2;
        } else {
            // Režim více relé: Ciferník vlevo, legenda a název vpravo
            int dialDiameter = Math.min(availableHeight - padding * 2, (int) (availableWidth * 0.65f));
            radius = dialDiameter / 2;
            centerX = padding + radius;
            centerY = availableHeight / 2;
        }

        // V tmavém režimu vyplnit vnitřní plochu ciferníku černou barvou
        if (isDarkMode) {
            canvas.drawCircle(centerX, centerY, radius, pBackground);
        }

        // 1. Vykreslení časových výsečí POD čísly a ryskami
        drawTimeArcs(canvas);

        // 2. Vykreslení kružnice a rysek (včetně půlhodin) NAD výsečemi
        drawClock(canvas);

        // 3. Vykreslení čísel NAD výsečemi
        drawNumbers(canvas);

        // 4. Vykreslení ikony zvonečku notifikací na výsečích
        drawNotificationBells(canvas);

        // 5. Vykreslení ručičky času (pokud je pro danou skupinu aktivní)
        drawHand(canvas);

        // 6. Vykreslení legendy vpravo od ciferníku (pouze pro více relé)
        if (activeTypesCount > 1) {
            drawLegend(canvas);
        }
    }


    /**
     * Vykreslí obvodovou kružnici a 48 rysek (24 hodinových + 24 půlhodinových)
     *
     * @param canvas plátno pro kreslení
     */
    private void drawClock(Canvas canvas) {
        int tickCount = 48; // 24 hodinových rysek + 24 půlhodinových rysek

        for (int i = 0; i < tickCount; i++) {
            double angle = Math.toRadians(i * (360.0 / tickCount));
            boolean isFullHour = (i % 2 == 0);
            int tickLength = isFullHour ? dpToPx(5) : dpToPx(3);
            pClock.setStrokeWidth(isFullHour ? dpToPx(1.5f) : dpToPx(1.0f));

            int startX = (int) (centerX + radius * Math.cos(angle));
            int startY = (int) (centerY + radius * Math.sin(angle));
            int endX = (int) (centerX + (radius - tickLength) * Math.cos(angle));
            int endY = (int) (centerY + (radius - tickLength) * Math.sin(angle));

            canvas.drawLine(startX, startY, endX, endY, pClock);
        }

        pClock.setStrokeWidth(dpToPx(1.5f));
        canvas.drawCircle(centerX, centerY, radius, pClock);
    }


    /**
     * Vykreslí čísla obvodu ciferníku (0 až 23)
     *
     * @param canvas plátno pro kreslení
     */
    private void drawNumbers(Canvas canvas) {
        int textRadius = radius - dpToPx(11);
        pNumbers.setTextSize(dpToPx(9.5f));

        for (int i = 0; i < 24; i++) {
            double angle = Math.toRadians(270 + i * 15);
            float x = (float) (centerX + textRadius * Math.cos(angle));
            float y = (float) (centerY + textRadius * Math.sin(angle)) + (pNumbers.getTextSize() / 3f);
            canvas.drawText(String.valueOf(i), x, y, pNumbers);
        }
    }


    /**
     * Vykreslí časové výseče HDO pro jednotlivá relé (TUV, TAR, PV)
     *
     * @param canvas plátno pro kreslení
     */
    private void drawTimeArcs(Canvas canvas) {
        if (models.isEmpty()) return;

        showTUV = false;
        showTAR = false;
        showPV = false;

        ArrayList<HdoModel> tuvModels = new ArrayList<>();
        ArrayList<HdoModel> tarModels = new ArrayList<>();
        ArrayList<HdoModel> pvModels = new ArrayList<>();

        for (HdoModel model : models) {
            RelayType type = getRelayType(model.getRele());
            if (type == RelayType.TUV) {
                tuvModels.add(model);
                showTUV = true;
            } else if (type == RelayType.TAR) {
                tarModels.add(model);
                showTAR = true;
            } else if (type == RelayType.PV) {
                pvModels.add(model);
                showPV = true;
            } else {
                tarModels.add(model);
                showTAR = true;
            }
        }

        calculateRingOffsets();

        if (showTUV) {
            for (HdoModel model : tuvModels) {
                drawSingleArc(canvas, model, tuvOffset, pTimeTUV, tuvModels);
            }
        }

        if (showTAR) {
            for (HdoModel model : tarModels) {
                drawSingleArc(canvas, model, tarOffset, pTimeTAR, tarModels);
            }
        }

        if (showPV) {
            for (HdoModel model : pvModels) {
                drawSingleArc(canvas, model, pvOffset, pTimePV, pvModels);
            }
        }
    }


    /**
     * Vypočítá odsazení prstenců pro jednotlivé typy relé
     */
    private void calculateRingOffsets() {
        tuvOffset = 0;
        tarOffset = 0;
        pvOffset = 0;
        smallerUnit = (int) (radius * 0.28);

        if (activeTypesCount > 1) {
            int currentOffsetIndex = 0;
            if (showTUV) {
                currentOffsetIndex++;
            }
            if (showTAR) {
                tarOffset = currentOffsetIndex * smallerUnit;
                currentOffsetIndex++;
            }
            if (showPV) {
                pvOffset = currentOffsetIndex * smallerUnit;
            }
        }
    }


    /**
     * Vykreslí jednu kruhovou výseč pro daný HDO interval s plynulým navázáním v půlnoci
     *
     * @param canvas         plátno pro kreslení
     * @param model          HDO model
     * @param offset         odsazení prstence od vnějšího obvodu
     * @param paint          štětec s příslušnou barvou relé
     * @param categoryModels seznam modelů stejné kategorie relé
     */
    private void drawSingleArc(Canvas canvas, HdoModel model, int offset, Paint paint, List<HdoModel> categoryModels) {
        float startAngle = convertTimeToAngle(model.getTimeFrom());
        float endAngle = convertTimeToAngle(model.getTimeUntil());
        float sweepAngle = endAngle - startAngle;
        if (sweepAngle <= 0) sweepAngle += 360f;

        boolean seamlessStart = isSeamlessAdjoining(model, categoryModels, false);
        boolean seamlessEnd = isSeamlessAdjoining(model, categoryModels, true);

        float gapStart = seamlessStart ? 0f : 0.3f;
        float gapEnd = seamlessEnd ? 0f : 0.3f;

        if (sweepAngle > (gapStart + gapEnd)) {
            startAngle += gapStart;
            sweepAngle -= (gapStart + gapEnd);
        }

        if (seamlessEnd) {
            sweepAngle += 0.3f;
        }

        RectF oval = new RectF(centerX - radius + offset, centerY - radius + offset, centerX + radius - offset, centerY + radius - offset);
        canvas.drawArc(oval, startAngle, sweepAngle, true, paint);
    }


    private boolean isSeamlessAdjoining(HdoModel model, List<HdoModel> categoryModels, boolean isEnd) {
        if (categoryModels == null || model == null) return false;
        String timeToCheck = isEnd ? model.getTimeUntil() : model.getTimeFrom();
        if (timeToCheck == null || timeToCheck.isEmpty()) return false;

        boolean isMidnight = "24:00".equals(timeToCheck) || "00:00".equals(timeToCheck) || "0:00".equals(timeToCheck);

        for (HdoModel other : categoryModels) {
            if (other == model) continue;
            String otherTime = isEnd ? other.getTimeFrom() : other.getTimeUntil();
            if (otherTime == null) continue;

            boolean otherIsMidnight = "24:00".equals(otherTime) || "00:00".equals(otherTime) || "0:00".equals(otherTime);

            if (isMidnight && otherIsMidnight) {
                return true;
            }
            if (timeToCheck.equalsIgnoreCase(otherTime)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Vykreslí ikony zvonečků u začátků a konců HDO intervalů, které mají aktivní notifikaci
     *
     * @param canvas plátno pro kreslení
     */
    private void drawNotificationBells(Canvas canvas) {
        if (models.isEmpty() || bellDrawable == null) return;

        calculateRingOffsets();
        int bellSize = dpToPx(14);

        for (HdoModel model : models) {
            if (model.getNotifyStart() != 1 && model.getNotifyEnd() != 1) {
                continue;
            }

            RelayType type = getRelayType(model.getRele());
            int offset = 0;
            if (activeTypesCount > 1) {
                if (type == RelayType.TUV) offset = tuvOffset;
                else if (type == RelayType.TAR) offset = tarOffset;
                else if (type == RelayType.PV) offset = pvOffset;
            }

            float ringCenterRadius = getRingCenterRadius(activeTypesCount, offset, smallerUnit);

            if (model.getNotifyStart() == 1) {
                float angle = convertTimeToAngle(model.getTimeFrom());
                drawBellAtAngle(canvas, angle, ringCenterRadius, bellSize);
            }

            if (model.getNotifyEnd() == 1) {
                float angle = convertTimeToAngle(model.getTimeUntil());
                drawBellAtAngle(canvas, angle, ringCenterRadius, bellSize);
            }
        }
    }


    /**
     * Vypočítá středový poloměr prstence pro umístění ikony zvonečku
     *
     * @param activeTypesCount počet aktivních typů relé
     * @param offset           odsazení prstence od okraje
     * @param smallerUnit      krok zmenšení prstence
     * @return středový poloměr prstence v pixelech
     */
    private float getRingCenterRadius(int activeTypesCount, int offset, int smallerUnit) {
        float ringCenterRadius;
        if (activeTypesCount > 1) {
            if (offset == 0) {
                // Na vnějším prstenci posunout zvoneček více ke středu, aby nepřekrýval čísla hodin
                ringCenterRadius = radius - smallerUnit * 0.72f;
            } else {
                ringCenterRadius = radius - offset - smallerUnit / 2f;
            }
        } else {
            // V režimu jednoho relé posunout zvoneček více ke středu
            ringCenterRadius = radius * 0.50f;
        }
        return ringCenterRadius;
    }


    /**
     * Vykreslí ikonu zvonečku na zadaném úhlu a poloměru prstence
     *
     * @param canvas     plátno pro kreslení
     * @param angle      úhel v stupních
     * @param ringRadius poloměr umístění zvonečku
     * @param bellSize   velikost zvonečku v pixelech
     */
    private void drawBellAtAngle(Canvas canvas, float angle, float ringRadius, int bellSize) {
        double radians = Math.toRadians(angle);
        float bellX = (float) (centerX + ringRadius * Math.cos(radians));
        float bellY = (float) (centerY + ringRadius * Math.sin(radians));

        int half = bellSize / 2;
        bellDrawable.setBounds(
                Math.round(bellX - half),
                Math.round(bellY - half),
                Math.round(bellX + half),
                Math.round(bellY + half)
        );
        bellDrawable.draw(canvas);
    }


    /**
     * Vykreslí červenou hodinovou ručičku ukazuící aktuální čas elektroměru
     *
     * @param canvas plátno pro kreslení
     */
    private void drawHand(Canvas canvas) {
        if (!showClockHand || currentMeterMinutes < 0) return;

        int hours = currentMeterMinutes / 60;
        int minutes = currentMeterMinutes % 60;

        float angle = 270f + (hours * 15f) + (minutes * 0.25f);
        double radians = Math.toRadians(angle);

        int handLength = radius - dpToPx(6);
        float endX = (float) (centerX + handLength * Math.cos(radians));
        float endY = (float) (centerY + handLength * Math.sin(radians));

        canvas.drawLine(centerX, centerY, endX, endY, pTick);
        canvas.drawCircle(centerX, centerY, dpToPx(3), pTick);
    }


    /**
     * Vykreslí název skupiny a barevnou legendu typů relé vpravo od ciferníku
     *
     * @param canvas plátno pro kreslení
     */
    private void drawLegend(Canvas canvas) {
        pTitle.setTextAlign(Paint.Align.LEFT);

        int legendSize = dpToPx(10);
        int legendPadding = dpToPx(8);
        int legendTextPadding = dpToPx(6);

        int legendX = centerX + radius + dpToPx(16);
        int legendY = centerY - radius + dpToPx(4);

        boolean hasTitle = groupTitle != null && !groupTitle.trim().isEmpty();
        if (hasTitle) {
            canvas.drawText(groupTitle, legendX, legendY + dpToPx(14), pTitle);
            legendY += dpToPx(24);
        }

        if (showTUV) {
            RectF oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
            canvas.drawArc(oval, 0, 360, true, pTimeTUV);
            canvas.drawText("Čas TUV", legendX + legendSize + legendTextPadding, legendY + legendSize - dpToPx(1), pLegend);
            legendY += legendSize + legendPadding;
        }

        if (showTAR) {
            RectF oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
            canvas.drawArc(oval, 0, 360, true, pTimeTAR);
            canvas.drawText("Čas TAR", legendX + legendSize + legendTextPadding, legendY + legendSize - dpToPx(1), pLegend);
            legendY += legendSize + legendPadding;
        }

        if (showPV) {
            RectF oval = new RectF(legendX, legendY, legendX + legendSize, legendY + legendSize);
            canvas.drawArc(oval, 0, 360, true, pTimePV);
            canvas.drawText("Čas PV", legendX + legendSize + legendTextPadding, legendY + legendSize - dpToPx(1), pLegend);
        }
    }


    /**
     * Přepočítá časový řetězec HH:mm na úhel ve stupních (00:00 = 270°)
     *
     * @param time čas ve formátu HH:mm
     * @return úhel ve stupních
     */
    private float convertTimeToAngle(String time) {
        if (time == null || time.isEmpty()) return 270f;
        try {
            String[] parts = time.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return 270f + (hours * 15f) + (minutes * 0.25f);
        } catch (Exception e) {
            return 270f;
        }
    }


    /**
     * Určí typ relé podle názvu pro účely barevného odlišení a zařazení do prstenců
     *
     * @param rele název relé
     * @return typ relé {@link RelayType}
     */
    private RelayType getRelayType(String rele) {
        if (rele == null || rele.isEmpty()) {
            return RelayType.UNKNOWN;
        }
        String upper = rele.toUpperCase(Locale.ROOT);

        boolean isPV = upper.contains("PV") || upper.contains("FVE") || upper.contains("FOTOVOLT");
        boolean isTUV = upper.contains("TUV") || upper.contains("AKU") || upper.contains("BOJLER");

        if (isPV && !isTUV) {
            return RelayType.PV;
        }
        if (isTUV) {
            return RelayType.TUV;
        }
        return RelayType.TAR;
    }


    /**
     * Přepočítá hodnutu dp na pixely
     *
     * @param dp hodnota v dp
     * @return hodnota v pixelech
     */
    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
