package cz.xlisto.elektrodroid.modules.widgets;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.radiobutton.MaterialRadioButton;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cz.xlisto.elektrodroid.MainActivity;
import cz.xlisto.elektrodroid.R;
import cz.xlisto.elektrodroid.models.SubscriptionPointModel;


/**
 * Konfigurační aktivita Material 3 pro prvotní nastavení i pozdější úpravu jedné konkrétní
 * instance domovského widgetu identifikované přes {@link AppWidgetManager#EXTRA_APPWIDGET_ID}.
 * <p>
 * Umožňuje nezávislý výběr odběrného místa a u widgetu aktuální ceny také volbu typu ceny
 * (celková / obchodní) a započítání DPH. Změna konfigurace se vždy ukládá výhradně pro dané
 * {@code appWidgetId} bez ovlivnění jiných instancí nebo globálního výběru hlavní aplikace.
 */
public class WidgetConfigActivity extends AppCompatActivity {

    /**
     * Od tohoto počtu odběrných míst se místo seznamu karet zobrazí rozbalovací {@link Spinner}.
     */
    private static final int SPINNER_THRESHOLD = 5;

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetType widgetType = WidgetType.CURRENT_PRICE;
    private WidgetConfigStore configStore;

    private final List<SubscriptionPointModel> subscriptionPoints = new ArrayList<>();
    private SubscriptionPointAdapter pointAdapter;
    private SubscriptionPointSpinnerAdapter spinnerAdapter;
    private long selectedPointId = -1L;

    private RecyclerView rvPoints;
    private MaterialCardView cardPointSpinner;
    private Spinner spPoints;
    private RadioGroup rgPriceMode;
    private RadioGroup rgVatMode;
    private MaterialButton btnSave;
    private LinearLayout layoutContent;
    private MaterialCardView cardEmptyPoints;

    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Výchozí výsledek při zrušení nebo stisku Zpět musí zůstat RESULT_CANCELED
        setResult(RESULT_CANCELED);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_widget_config);

        appWidgetId = extractAppWidgetId(getIntent());
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        configStore = new WidgetConfigStore(this);
        WidgetConfig existingConfig = configStore.loadConfig(appWidgetId);
        widgetType = resolveWidgetType(appWidgetId, existingConfig);

        applyWindowInsets();
        bindViews(existingConfig);
        loadSubscriptionPoints(existingConfig);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        saveExecutor.shutdownNow();
    }


    private int extractAppWidgetId(@Nullable Intent intent) {
        if (intent == null) {
            return AppWidgetManager.INVALID_APPWIDGET_ID;
        }
        return intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
    }


    @NonNull
    private WidgetType resolveWidgetType(int widgetId, @Nullable WidgetConfig existingConfig) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        AppWidgetProviderInfo info = manager.getAppWidgetInfo(widgetId);
        if (info != null) {
            WidgetType fromInfo = WidgetType.fromComponentName(info.provider);
            if (fromInfo != null) {
                return fromInfo;
            }
        }
        if (existingConfig != null) {
            return existingConfig.widgetType();
        }
        return WidgetType.CURRENT_PRICE;
    }


    private void applyWindowInsets() {
        View root = findViewById(R.id.widget_config_root);
        View appBar = findViewById(R.id.appbar_widget_config);
        View scrollView = findViewById(R.id.scroll_widget_config);
        View actionsBar = findViewById(R.id.layout_widget_config_actions);

        final int appBarPaddingLeft = appBar.getPaddingLeft();
        final int appBarPaddingTop = appBar.getPaddingTop();
        final int appBarPaddingRight = appBar.getPaddingRight();
        final int appBarPaddingBottom = appBar.getPaddingBottom();

        final int scrollPaddingLeft = scrollView.getPaddingLeft();
        final int scrollPaddingTop = scrollView.getPaddingTop();
        final int scrollPaddingRight = scrollView.getPaddingRight();
        final int scrollPaddingBottom = scrollView.getPaddingBottom();

        final int actionsPaddingLeft = actionsBar.getPaddingLeft();
        final int actionsPaddingTop = actionsBar.getPaddingTop();
        final int actionsPaddingRight = actionsBar.getPaddingRight();
        final int actionsPaddingBottom = actionsBar.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            appBar.setPadding(
                    appBarPaddingLeft + bars.left,
                    appBarPaddingTop + bars.top,
                    appBarPaddingRight + bars.right,
                    appBarPaddingBottom
            );
            scrollView.setPadding(
                    scrollPaddingLeft + bars.left,
                    scrollPaddingTop,
                    scrollPaddingRight + bars.right,
                    scrollPaddingBottom
            );
            actionsBar.setPadding(
                    actionsPaddingLeft + bars.left,
                    actionsPaddingTop,
                    actionsPaddingRight + bars.right,
                    actionsPaddingBottom + bars.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }


    private void bindViews(@Nullable WidgetConfig existingConfig) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_widget_config);
        toolbar.setTitle(R.string.widget_config_title);
        toolbar.setSubtitle(widgetType.getLabelResId());
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvDescription = findViewById(R.id.tv_widget_config_description);
        tvDescription.setText(widgetType.getDescriptionResId());

        layoutContent = findViewById(R.id.layout_widget_config_content);
        LinearLayout layoutPriceOptions = findViewById(R.id.layout_price_options);
        cardEmptyPoints = findViewById(R.id.card_empty_points);
        rgPriceMode = findViewById(R.id.rg_price_mode);
        rgVatMode = findViewById(R.id.rg_vat_mode);
        btnSave = findViewById(R.id.btn_widget_save);
        MaterialButton btnCancel = findViewById(R.id.btn_widget_cancel);
        MaterialButton btnOpenApp = findViewById(R.id.btn_open_app);

        rvPoints = findViewById(R.id.rv_widget_subscription_points);
        rvPoints.setLayoutManager(new LinearLayoutManager(this));
        pointAdapter = new SubscriptionPointAdapter(subscriptionPoints, point -> {
            selectedPointId = point.getId();
            pointAdapter.setSelectedPointId(selectedPointId);
            btnSave.setEnabled(true);
        });
        rvPoints.setAdapter(pointAdapter);

        cardPointSpinner = findViewById(R.id.card_widget_subscription_point_spinner);
        spPoints = findViewById(R.id.sp_widget_subscription_points);
        spinnerAdapter = new SubscriptionPointSpinnerAdapter(subscriptionPoints);
        spPoints.setAdapter(spinnerAdapter);
        spPoints.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < subscriptionPoints.size()) {
                    selectedPointId = subscriptionPoints.get(position).getId();
                    spinnerAdapter.setSelectedPointId(selectedPointId);
                    btnSave.setEnabled(true);
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Výběr odběrného místa zůstává beze změny
            }
        });

        if (widgetType.supportsPriceOptions()) {
            layoutPriceOptions.setVisibility(View.VISIBLE);
            boolean showTotal = existingConfig == null || existingConfig.showTotalPrice();
            boolean includeVat = existingConfig == null || existingConfig.includeVat();
            rgPriceMode.check(showTotal ? R.id.rb_price_total : R.id.rb_price_commercial);
            rgVatMode.check(includeVat ? R.id.rb_vat_include : R.id.rb_vat_exclude);
        } else {
            layoutPriceOptions.setVisibility(View.GONE);
        }

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveAndFinish());
        btnOpenApp.setOnClickListener(v -> {
            Intent openMain = new Intent(this, MainActivity.class);
            openMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(openMain);
            finish();
        });
    }


    private void loadSubscriptionPoints(@Nullable WidgetConfig existingConfig) {
        List<SubscriptionPointModel> loaded = WidgetPointResolver.loadAllSubscriptionPoints(this);
        subscriptionPoints.clear();
        subscriptionPoints.addAll(loaded);

        if (subscriptionPoints.isEmpty()) {
            layoutContent.setVisibility(View.GONE);
            cardEmptyPoints.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            return;
        }

        layoutContent.setVisibility(View.VISIBLE);
        cardEmptyPoints.setVisibility(View.GONE);

        WidgetPointResolver.ResolutionResult existingResolution = WidgetPointResolver.resolve(
                existingConfig, subscriptionPoints);
        if (existingResolution.status() == WidgetPointResolver.ResolutionStatus.RESOLVED
                && existingResolution.point() != null) {
            selectedPointId = existingResolution.point().getId();
        } else {
            selectedPointId = subscriptionPoints.get(0).getId();
        }

        boolean useSpinner = subscriptionPoints.size() >= SPINNER_THRESHOLD;
        if (useSpinner) {
            rvPoints.setVisibility(View.GONE);
            cardPointSpinner.setVisibility(View.VISIBLE);
            spinnerAdapter.setSelectedPointId(selectedPointId);
            spinnerAdapter.notifyDataSetChanged();
            spPoints.setSelection(findSelectedPointIndex(), false);
        } else {
            rvPoints.setVisibility(View.VISIBLE);
            cardPointSpinner.setVisibility(View.GONE);
            pointAdapter.setSelectedPointId(selectedPointId);
        }
        btnSave.setEnabled(selectedPointId > 0L);
    }


    private int findSelectedPointIndex() {
        for (int i = 0; i < subscriptionPoints.size(); i++) {
            if (subscriptionPoints.get(i).getId() == selectedPointId) {
                return i;
            }
        }
        return 0;
    }


    private void saveAndFinish() {
        SubscriptionPointModel selectedPoint = findSelectedPoint();
        if (selectedPoint == null) {
            Toast.makeText(this, R.string.widget_config_select_point_hint, Toast.LENGTH_SHORT).show();
            return;
        }

        boolean showTotalPrice = !widgetType.supportsPriceOptions()
                || rgPriceMode.getCheckedRadioButtonId() == R.id.rb_price_total;
        boolean includeVat = !widgetType.supportsPriceOptions()
                || rgVatMode.getCheckedRadioButtonId() == R.id.rb_vat_include;

        WidgetConfig newConfig = new WidgetConfig(
                appWidgetId,
                widgetType,
                selectedPoint.getId(),
                selectedPoint.getMilins(),
                selectedPoint.getName(),
                showTotalPrice,
                includeVat
        );

        btnSave.setEnabled(false);
        configStore.saveConfig(newConfig);

        saveExecutor.execute(() -> {
            WidgetUpdateManager.updateSingleWidgetNow(getApplicationContext(), widgetType, appWidgetId);
            runOnUiThread(() -> {
                Intent resultValue = new Intent();
                resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                setResult(RESULT_OK, resultValue);
                finish();
            });
        });
    }


    @Nullable
    private SubscriptionPointModel findSelectedPoint() {
        for (SubscriptionPointModel point : subscriptionPoints) {
            if (point.getId() == selectedPointId) {
                return point;
            }
        }
        return null;
    }


    private interface OnPointSelectedListener {

        void onPointSelected(@NonNull SubscriptionPointModel point);

    }


    private static final class SubscriptionPointAdapter extends RecyclerView.Adapter<SubscriptionPointAdapter.PointViewHolder> {

        private final List<SubscriptionPointModel> items;
        private final OnPointSelectedListener listener;
        private long selectedPointId = -1L;


        SubscriptionPointAdapter(@NonNull List<SubscriptionPointModel> items,
                                 @NonNull OnPointSelectedListener listener) {
            this.items = items;
            this.listener = listener;
        }


        void setSelectedPointId(long newSelectedPointId) {
            long previousId = this.selectedPointId;
            this.selectedPointId = newSelectedPointId;
            for (int i = 0; i < items.size(); i++) {
                long id = items.get(i).getId();
                if (id == previousId || id == newSelectedPointId) {
                    notifyItemChanged(i);
                }
            }
        }


        @NonNull
        @Override
        public PointViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_widget_subscription_point, parent, false);
            return new PointViewHolder(view);
        }


        @Override
        public void onBindViewHolder(@NonNull PointViewHolder holder, int position) {
            SubscriptionPointModel item = items.get(position);
            boolean isSelected = item.getId() == selectedPointId;

            holder.tvName.setText(item.getName());
            holder.rbSelected.setChecked(isSelected);

            String subtitle = buildPointSubtitle(holder.itemView.getContext(), item);
            if (subtitle.isEmpty()) {
                holder.tvSubtitle.setVisibility(View.GONE);
            } else {
                holder.tvSubtitle.setVisibility(View.VISIBLE);
                holder.tvSubtitle.setText(subtitle);
            }

            int strokeColor = MaterialColors.getColor(
                    holder.cardView,
                    isSelected ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOutlineVariant
            );
            int bgColor = MaterialColors.getColor(
                    holder.cardView,
                    isSelected ? com.google.android.material.R.attr.colorSecondaryContainer : com.google.android.material.R.attr.colorSurfaceContainerLow
            );
            holder.cardView.setStrokeColor(strokeColor);
            holder.cardView.setStrokeWidth(isSelected ? dpToPx(holder.cardView, 2) : dpToPx(holder.cardView, 1));
            holder.cardView.setCardBackgroundColor(bgColor);

            holder.cardView.setOnClickListener(v -> listener.onPointSelected(item));
        }


        @Override
        public int getItemCount() {
            return items.size();
        }


        private static int dpToPx(@NonNull View view, int dp) {
            return Math.round(dp * view.getResources().getDisplayMetrics().density);
        }


        static final class PointViewHolder extends RecyclerView.ViewHolder {

            final MaterialCardView cardView;
            final MaterialRadioButton rbSelected;
            final TextView tvName;
            final TextView tvSubtitle;


            PointViewHolder(@NonNull View itemView) {
                super(itemView);
                cardView = itemView.findViewById(R.id.card_subscription_point);
                rbSelected = itemView.findViewById(R.id.rb_point_selected);
                tvName = itemView.findViewById(R.id.tv_point_name);
                tvSubtitle = itemView.findViewById(R.id.tv_point_subtitle);
            }

        }

    }


    /**
     * Sestaví doplňkový podtitulek odběrného místa (číslo elektroměru a popis).
     * Používá {@code length() > 0}, protože {@code CharSequence.isEmpty()} vyžaduje API 35+ (minSdk je 24).
     */
    @NonNull
    @SuppressWarnings("SizeReplaceableByIsEmpty")
    private static String buildPointSubtitle(@NonNull Context context, @NonNull SubscriptionPointModel point) {
        StringBuilder sb = new StringBuilder();
        if (point.getNumberElectricMeter() != null && !point.getNumberElectricMeter().trim().isEmpty()) {
            sb.append(context.getString(
                    R.string.widget_config_point_meter_format,
                    point.getNumberElectricMeter().trim()
            ));
        }
        if (point.getDescription() != null && !point.getDescription().trim().isEmpty()) {
            if (sb.length() > 0) {
                sb.append(" • ");
            }
            sb.append(point.getDescription().trim());
        }
        return sb.toString();
    }


    /**
     * Adaptér pro výběr odběrného místa v rozbalovacím {@link Spinner}, používaný při 5 a více
     * odběrných místech. Zobrazuje název místa a případný podtitulek (elektroměr a popis).
     */
    private static final class SubscriptionPointSpinnerAdapter extends BaseAdapter {

        private final List<SubscriptionPointModel> items;
        private long selectedPointId = -1L;


        SubscriptionPointSpinnerAdapter(@NonNull List<SubscriptionPointModel> items) {
            this.items = items;
        }


        void setSelectedPointId(long selectedPointId) {
            this.selectedPointId = selectedPointId;
        }


        @Override
        public int getCount() {
            return items.size();
        }


        @Override
        public SubscriptionPointModel getItem(int position) {
            return items.get(position);
        }


        @Override
        public long getItemId(int position) {
            return items.get(position).getId();
        }


        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return bindSpinnerItemView(position, convertView, parent, false);
        }


        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return bindSpinnerItemView(position, convertView, parent, true);
        }


        @NonNull
        private View bindSpinnerItemView(int position,
                                         @Nullable View convertView,
                                         @NonNull ViewGroup parent,
                                         boolean isDropDown) {
            View view = convertView;
            if (view == null) {
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_widget_subscription_point_spinner, parent, false);
            }

            SubscriptionPointModel item = items.get(position);
            TextView tvName = view.findViewById(R.id.tv_spinner_point_name);
            TextView tvSubtitle = view.findViewById(R.id.tv_spinner_point_subtitle);

            tvName.setText(item.getName());
            String subtitle = buildPointSubtitle(parent.getContext(), item);
            if (subtitle.isEmpty()) {
                tvSubtitle.setVisibility(View.GONE);
            } else {
                tvSubtitle.setVisibility(View.VISIBLE);
                tvSubtitle.setText(subtitle);
            }

            boolean isSelected = isDropDown && item.getId() == selectedPointId;
            int nameColor = MaterialColors.getColor(
                    view,
                    isSelected ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOnSurface
            );
            tvName.setTextColor(nameColor);
            if (isSelected) {
                int selectedBg = MaterialColors.getColor(
                        view,
                        com.google.android.material.R.attr.colorSecondaryContainer
                );
                view.setBackgroundColor(selectedBg);
            } else {
                view.setBackgroundColor(Color.TRANSPARENT);
            }

            return view;
        }

    }

}
