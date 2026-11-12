package cz.xlisto.elektrodroid.ownview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.List;

import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.format.DecimalFormatHelper;
import cz.xlisto.elektrodroid.modules.pricelist.models.PriceListComparisonResult;

/**
 * Custom View pro grafické porovnání ceníků – podporuje vodorovné sloupcové grafy i skládané sloupce (Struktura ceny).
 * Automaticky zalamuje dlouhé názvy ceníků a subtitulky na více řádků bez ořezávání textu.
 * Dynamicky přizpůsobuje barvy textu a prvků pro světlý i tmavý režim (Light/Dark mode).
 */
public class PriceListCompareChartView extends View {

    /**
     * Typy grafu porovnání ceníků.
     */
    public enum ChartType {
        /**
         * Vodorovný sloupcový graf.
         */
        HORIZONTAL_BAR,
        /** Skládaný sloupcový graf (struktura ceny). */
        STACKED_BAR
    }


    /**
     * Zobrazované metriky pro grafické porovnání ceníků.
     */
    public enum DisplayMetric {
        /** Celkové roční náklady. */
        TOTAL_ANNUAL_COST,
        /** Průměrné měsíční náklady. */
        AVG_MONTHLY_COST,
        /** Cena za vysoký tarif (VT). */
        VT_PRICE,
        /** Cena za nízký tarif (NT). */
        NT_PRICE,
        /** Stálý měsíční plat. */
        FIXED_MONTHLY_FEE,
        /** Regulovaná složka ceny. */
        REGULATED_COMPONENT,
        /** Neregulovaná (obchodní) složka ceny. */
        UNREGULATED_COMPONENT,
        /** Struktura ceny (skládaný sloupec). */
        PRICE_STRUCTURE_STACKED,
        /** Rozdíl oproti nejlevnějšímu ceníku. */
        DIFF_VS_CHEAPEST
    }

    private ChartType chartType = ChartType.HORIZONTAL_BAR;
    private DisplayMetric displayMetric = DisplayMetric.TOTAL_ANNUAL_COST;
    private List<PriceListComparisonResult> results = new ArrayList<>();

    private Paint textPaint;
    private Paint barPaint;
    private Paint merchantPaint;
    private Paint regulatedPaint;
    private Paint taxPaint;
    private Paint cheapestHighlightPaint;

    private float density;
    private final RectF tempRect = new RectF();

    private final int[] chartPalette = new int[]{
            0xFF1E88E5, 0xFF43A047, 0xFFFB8C00, 0xFF8E24AA,
            0xFFE53935, 0xFF00ACC1, 0xFF3949AB, 0xFFD81B60
    };

    public PriceListCompareChartView(Context context) {
        super(context);
        init(context);
    }

    public PriceListCompareChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PriceListCompareChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(12 * density);

        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);

        merchantPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        merchantPaint.setStyle(Paint.Style.FILL);
        merchantPaint.setColor(0xFF1E88E5); // Modrá - Obchodní

        regulatedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        regulatedPaint.setStyle(Paint.Style.FILL);
        regulatedPaint.setColor(0xFF43A047); // Zelená - Regulovaná

        taxPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        taxPaint.setStyle(Paint.Style.FILL);
        taxPaint.setColor(0xFFE53935); // Červená - Daně

        cheapestHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cheapestHighlightPaint.setStyle(Paint.Style.STROKE);
        cheapestHighlightPaint.setStrokeWidth(2 * density);
        cheapestHighlightPaint.setColor(0xFF2E7D32); // Zelený rámeček vítěze
    }


    /**
     * Nastaví data pro zobrazení v grafu a zvolenou zobrazenou metriku.
     * Automaticky určí odpovídající typ grafu (vodorovné nebo skládané sloupce),
     * přepočítá rozvržení a vyvolá překreslení View.
     *
     * @param results seznam výsledků porovnání ceníků
     * @param metric  zobrazovaná metrika
     */
    public void setData(List<PriceListComparisonResult> results, DisplayMetric metric) {
        this.results = results != null ? results : new ArrayList<>();
        this.displayMetric = metric;
        this.chartType = (metric == DisplayMetric.PRICE_STRUCTURE_STACKED) ? ChartType.STACKED_BAR : ChartType.HORIZONTAL_BAR;

        requestLayout();
        invalidate();
    }


    /**
     * Rozdělí zadaný text na více řádků tak, aby žádný řádek nepřesáhl maximální povolenou šířku.
     *
     * @param paint    objekt {@link Paint} použitý k měření šířky textu
     * @param text     text k rozdělení
     * @param maxWidth maximální šířka řádku v pixelech
     * @return seznam řádků textu
     */
    @SuppressWarnings("SizeReplaceableByIsEmpty")
    private List<String> splitTextToLines(Paint paint, String text, float maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) return lines;

        if (paint.measureText(text) <= maxWidth) {
            lines.add(text);
            return lines;
        }

        // Přednostní rozdělení před závorkou " (" pokud existuje
        int parenIdx = text.indexOf(" (");
        if (parenIdx > 0) {
            String part1 = text.substring(0, parenIdx).trim();
            String part2 = text.substring(parenIdx).trim();
            if (paint.measureText(part1) <= maxWidth && paint.measureText(part2) <= maxWidth) {
                lines.add(part1);
                lines.add(part2);
                return lines;
            }
        }

        // Obecné zalamování po slovech
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            // CharSequence.isEmpty() vyžaduje API level 35+ (minSdk je 24), proto je použit length() == 0
            //noinspection SizeReplaceableByIsEmpty
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else {
                String testLine = currentLine + " " + word;
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine.setLength(0);
                    currentLine.append(word);
                }
            }
        }
        // CharSequence.isEmpty() vyžaduje API level 35+ (minSdk je 24), proto je použit length() > 0
        //noinspection SizeReplaceableByIsEmpty
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }


    /**
     * Vypočítá výšku jednoho položkového bloku v grafu s ohledem na počet zalamovaných řádků titulku a subtitulku.
     *
     * @param item     položka s výsledkem porovnání ceníku
     * @param maxWidth maximální dostupná šířka pro text
     * @return požadovaná výška položky v pixelech
     */
    private float calculateItemHeight(PriceListComparisonResult item, float maxWidth) {
        textPaint.setFakeBoldText(true);
        String title = item.getPriceList().getNameWithPeriod();
        List<String> titleLines = splitTextToLines(textPaint, title, maxWidth);
        int titleCount = Math.max(1, titleLines.size());

        textPaint.setFakeBoldText(false);
        String pozeInfo = item.getPozeTypeDescription();
        String subtitle = item.getPriceList().getFirma() + " • " + item.getPriceList().getDistribuce() + (pozeInfo.isEmpty() ? "" : " • " + pozeInfo);
        List<String> subtitleLines = splitTextToLines(textPaint, subtitle, maxWidth);
        int subtitleCount = Math.max(1, subtitleLines.size());

        float textHeight = (titleCount + subtitleCount) * (15 * density);
        return textHeight + (54 * density);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        float paddingLeft = 12 * density;
        float paddingRight = 16 * density;
        float maxTextWidth = Math.max(100 * density, width - paddingLeft - paddingRight);

        float totalItemsHeight = 0;
        for (PriceListComparisonResult item : results) {
            totalItemsHeight += calculateItemHeight(item, maxTextWidth);
        }

        int headerHeight = (int) (20 * density);
        int legendHeight = (chartType == ChartType.STACKED_BAR) ? (int) (35 * density) : 0;

        int desiredHeight = (int) (headerHeight + totalItemsHeight + legendHeight + (20 * density));
        setMeasuredDimension(width, desiredHeight);
    }


    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int textColorPrimary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, ContextCompat.getColor(getContext(), R.color.color_axis));
        int textColorSecondary = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF757575);

        if (results == null || results.isEmpty()) {
            textPaint.setColor(textColorSecondary);
            canvas.drawText(getContext().getString(R.string.no_price_lists_to_display), 20 * density, 40 * density, textPaint);
            return;
        }

        int width = getWidth();
        float paddingLeft = 12 * density;
        float paddingRight = 16 * density;
        float maxTextWidth = Math.max(100 * density, width - paddingLeft - paddingRight);
        float startY = 20 * density;
        float barHeight = 22 * density;

        // Vykreslení legendy pro skládaný graf
        if (chartType == ChartType.STACKED_BAR) {
            drawLegend(canvas, paddingLeft, 18 * density, textColorSecondary);
            startY += 25 * density;
        }

        // Určení maximální hodnoty pro škálování šířky sloupců
        double maxValue = getMaxValue();
        if (maxValue <= 0) maxValue = 1.0;

        float maxBarWidth = width - paddingLeft - paddingRight - (130 * density);
        if (maxBarWidth < 100 * density) maxBarWidth = 100 * density;

        float currentTopY = startY;

        for (int i = 0; i < results.size(); i++) {
            PriceListComparisonResult item = results.get(i);
            float topY = currentTopY;

            // Popis ceníku (zalamovaný)
            textPaint.setFakeBoldText(true);
            textPaint.setColor(textColorPrimary);
            String title = item.getPriceList().getNameWithPeriod();
            List<String> titleLines = splitTextToLines(textPaint, title, maxTextWidth);

            float textY = topY + (14 * density);
            for (String line : titleLines) {
                canvas.drawText(line, paddingLeft, textY, textPaint);
                textY += (15 * density);
            }

            // Subtitulek (zalamovaný)
            textPaint.setFakeBoldText(false);
            textPaint.setColor(textColorSecondary);
            String pozeInfo = item.getPozeTypeDescription();
            String subtitle = item.getPriceList().getFirma() + " • " + item.getPriceList().getDistribuce() + (pozeInfo.isEmpty() ? "" : " • " + pozeInfo);
            List<String> subtitleLines = splitTextToLines(textPaint, subtitle, maxTextWidth);

            for (String line : subtitleLines) {
                canvas.drawText(line, paddingLeft, textY, textPaint);
                textY += (15 * density);
            }

            float barStartY = textY + (2 * density);

            if (chartType == ChartType.HORIZONTAL_BAR) {
                double val = getValueForMetric(item);
                float barWidth = (float) ((val / maxValue) * maxBarWidth);
                if (barWidth < 4 * density && val > 0) barWidth = 4 * density;

                barPaint.setColor(chartPalette[i % chartPalette.length]);
                tempRect.set(paddingLeft, barStartY, paddingLeft + barWidth, barStartY + barHeight);
                canvas.drawRoundRect(tempRect, 4 * density, 4 * density, barPaint);

                if (item.isCheapest()) {
                    canvas.drawRoundRect(tempRect, 4 * density, 4 * density, cheapestHighlightPaint);
                }

                // Text s hodnotou vedle sloupce
                String valueText = formatMetricValue(val);
                textPaint.setColor(textColorPrimary);
                textPaint.setFakeBoldText(true);
                canvas.drawText(valueText, paddingLeft + barWidth + (8 * density), barStartY + (16 * density), textPaint);

            } else { // STACKED_BAR
                double merchantCost = item.getMerchantTotalCost();
                double regulatedCost = item.getRegulatedTotalCost();
                double taxCost = item.getElectricityTax();

                float merchantW = (float) ((merchantCost / maxValue) * maxBarWidth);
                float regulatedW = (float) ((regulatedCost / maxValue) * maxBarWidth);
                float taxW = (float) ((taxCost / maxValue) * maxBarWidth);

                float currX = paddingLeft;

                // Obchodní
                if (merchantW > 0) {
                    tempRect.set(currX, barStartY, currX + merchantW, barStartY + barHeight);
                    canvas.drawRect(tempRect, merchantPaint);
                    currX += merchantW;
                }
                // Regulovaná
                if (regulatedW > 0) {
                    tempRect.set(currX, barStartY, currX + regulatedW, barStartY + barHeight);
                    canvas.drawRect(tempRect, regulatedPaint);
                    currX += regulatedW;
                }
                // Daně
                if (taxW > 0) {
                    tempRect.set(currX, barStartY, currX + taxW, barStartY + barHeight);
                    canvas.drawRect(tempRect, taxPaint);
                    currX += taxW;
                }

                String valueText = DecimalFormatHelper.df0.format(item.getTotalWithDph()) + " Kč";
                textPaint.setColor(textColorPrimary);
                textPaint.setFakeBoldText(true);
                canvas.drawText(valueText, currX + (8 * density), barStartY + (16 * density), textPaint);
            }

            currentTopY = barStartY + barHeight + (16 * density);
        }
    }


    /**
     * Vykreslí barevnou legendu složek ceny (Obchodní, Regulovaná, Daně) pro skládaný graf.
     *
     * @param canvas    plátno pro kreslení
     * @param startX    počáteční X souřadnice legendy
     * @param startY    počáteční Y souřadnice legendy
     * @param textColor barva textu legendy
     */
    private void drawLegend(Canvas canvas, float startX, float startY, int textColor) {
        float x = startX;
        float boxSize = 12 * density;
        float spacing = 16 * density;

        textPaint.setColor(textColor);
        textPaint.setFakeBoldText(false);

        // Obchodní
        String textMerchant = getContext().getString(R.string.obchodni_slozka);
        canvas.drawRect(x, startY, x + boxSize, startY + boxSize, merchantPaint);
        canvas.drawText(textMerchant, x + boxSize + (4 * density), startY + (10 * density), textPaint);
        x += boxSize + (4 * density) + textPaint.measureText(textMerchant) + spacing;

        // Regulovaná
        String textRegulated = getContext().getString(R.string.regulovana_slozka);
        canvas.drawRect(x, startY, x + boxSize, startY + boxSize, regulatedPaint);
        canvas.drawText(textRegulated, x + boxSize + (4 * density), startY + (10 * density), textPaint);
        x += boxSize + (4 * density) + textPaint.measureText(textRegulated) + spacing;

        // Daně
        String textTax = getContext().getString(R.string.dane);
        canvas.drawRect(x, startY, x + boxSize, startY + boxSize, taxPaint);
        canvas.drawText(textTax, x + boxSize + (4 * density), startY + (10 * density), textPaint);
    }


    /**
     * Určí maximální hodnotu ze všech výsledků pro zadanou metriku, podle které se škáluje délka sloupců.
     *
     * @return maximální hodnota
     */
    private double getMaxValue() {
        double max = 0;
        for (PriceListComparisonResult res : results) {
            double val = (chartType == ChartType.STACKED_BAR) ? res.getTotalWithoutDph() : getValueForMetric(res);
            if (val > max) max = val;
        }
        return max;
    }


    /**
     * Vrátí číselnou hodnotu vybrané metriky z daného výsledku porovnání.
     *
     * @param res výsledek porovnání ceníku
     * @return číselná hodnota zobrazené metriky
     */
    private double getValueForMetric(PriceListComparisonResult res) {
        return switch (displayMetric) {
            case TOTAL_ANNUAL_COST, PRICE_STRUCTURE_STACKED -> res.getTotalWithDph();
            case AVG_MONTHLY_COST -> res.getAverageMonthlyCostWithDph();
            case VT_PRICE -> res.getPriceList().getCenaVT();
            case NT_PRICE -> res.getPriceList().getCenaNT();
            case FIXED_MONTHLY_FEE -> res.getPriceList().getMesicniPlat();
            case REGULATED_COMPONENT -> res.getRegulatedTotalCost();
            case UNREGULATED_COMPONENT -> res.getMerchantTotalCost();
            case DIFF_VS_CHEAPEST -> res.getDiffVsCheapest();
        };
    }


    /**
     * Formátuje číselnou hodnotu metriky do textového řetězce včetně příslušných měrových jednotek.
     *
     * @param val hodnota k formátování
     * @return formátovaný řetězec s měnovou nebo jednotkovou příponou
     */
    private String formatMetricValue(double val) {
        return switch (displayMetric) {
            case VT_PRICE, NT_PRICE -> DecimalFormatHelper.df2.format(val) + " Kč/MWh";
            case FIXED_MONTHLY_FEE, AVG_MONTHLY_COST ->
                    DecimalFormatHelper.df0.format(val) + " Kč/měs";
            default -> DecimalFormatHelper.df0.format(val) + " Kč";
        };
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            performClick();
            invalidate();
            return true;
        }
        return super.onTouchEvent(event);
    }
}
