package cz.xlisto.elektrodroid.modules.widgets;


import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
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

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private WidgetType widgetType = WidgetType.CURRENT_PRICE;
    private WidgetConfigStore configStore;

    private final List<SubscriptionPointModel> subscriptionPoints = new ArrayList<>();
    private SubscriptionPointAdapter pointAdapter;
    private long selectedPointId = -1L;

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
            return existingConfig.getWidgetType();
        }
        return WidgetType.CURRENT_PRICE;
    }


    private void applyWindowInsets() {
        View root = findViewById(R.id.widget_config_root);
        MaterialToolbar toolbar = findViewById(R.id.toolbar_widget_config);
        View actionsBar = findViewById(R.id.layout_widget_config_actions);

        final int actionsBottomPadding = actionsBar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            toolbar.setPadding(
                    toolbar.getPaddingLeft(),
                    bars.top,
                    toolbar.getPaddingRight(),
                    toolbar.getPaddingBottom()
            );
            actionsBar.setPadding(
                    actionsBar.getPaddingLeft(),
                    actionsBar.getPaddingTop(),
                    actionsBar.getPaddingRight(),
                    actionsBottomPadding + bars.bottom
            );
            return windowInsets;
        });
    }


    private void bindViews(@Nullable WidgetConfig existingConfig) {
        MaterialToolbar toolbar = findViewById(R.id.toolbar_widget_config);
        toolbar.setTitle(widgetType.getConfigTitleResId());
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

        RecyclerView rvPoints = findViewById(R.id.rv_widget_subscription_points);
        rvPoints.setLayoutManager(new LinearLayoutManager(this));
        pointAdapter = new SubscriptionPointAdapter(subscriptionPoints, point -> {
            selectedPointId = point.getId();
            pointAdapter.setSelectedPointId(selectedPointId);
            btnSave.setEnabled(true);
        });
        rvPoints.setAdapter(pointAdapter);

        if (widgetType.supportsPriceOptions()) {
            layoutPriceOptions.setVisibility(View.VISIBLE);
            boolean showTotal = existingConfig == null || existingConfig.isShowTotalPrice();
            boolean includeVat = existingConfig == null || existingConfig.isIncludeVat();
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

        pointAdapter.setSelectedPointId(selectedPointId);
        btnSave.setEnabled(selectedPointId > 0L);
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

            String subtitle = buildPointSubtitle(holder.itemView, item);
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


        @NonNull
        private static String buildPointSubtitle(@NonNull View view, @NonNull SubscriptionPointModel point) {
            StringBuilder sb = new StringBuilder();
            if (point.getNumberElectricMeter() != null && !point.getNumberElectricMeter().trim().isEmpty()) {
                sb.append(view.getContext().getString(
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

}
