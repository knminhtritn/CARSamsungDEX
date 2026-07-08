package com.thangtinstore.vf3dexopen;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {
    private static final int REQ_LOCATION = 1001;
    private static final String PREFS = "vf3dex_prefs";

    private SharedPreferences prefs;
    private LocationManager locationManager;
    private Location lastLocation;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private LinearLayout contentRow;
    private LinearLayout dashboardPanel;
    private LinearLayout centerPanel;
    private LinearLayout rightPanel;
    private LinearLayout toolbar;
    private TextView speedText;
    private TextView statusText;
    private TextView latLngText;
    private TextView accuracyText;
    private TextView headingText;
    private TextView timeText;
    private TextView vehicleNameText;
    private TextView centerAppChip;
    private TextView rightAppChip;
    private Spinner vehicleSpinner;
    private RoutePreviewView routePreviewView;

    private boolean dashboardVisible = true;
    private boolean centerVisible = true;
    private boolean rightVisible = true;
    private float dashboardWeight = 25f;
    private float centerWeight = 45f;
    private float rightWeight = 30f;

    private static final String[] VINFAST_MODELS = new String[]{
            "VF 3", "VF 5", "VF 6", "VF 7", "VF 8", "VF 9", "VF e34", "Minio Green", "Limo Green"
    };

    private static final String PREF_DASHBOARD_VISIBLE = "dashboard_visible";
    private static final String PREF_CENTER_VISIBLE = "center_visible";
    private static final String PREF_RIGHT_VISIBLE = "right_visible";
    private static final String PREF_DASHBOARD_WEIGHT = "dashboard_weight";
    private static final String PREF_CENTER_WEIGHT = "center_weight";
    private static final String PREF_RIGHT_WEIGHT = "right_weight";
    private static final String PREF_VEHICLE = "vehicle";
    private static final String PREF_CENTER_PACKAGE = "center_package";
    private static final String PREF_CENTER_LABEL = "center_label";
    private static final String PREF_CENTER_URL = "center_url";
    private static final String PREF_RIGHT_PACKAGE = "right_package";
    private static final String PREF_RIGHT_LABEL = "right_label";
    private static final String PREF_RIGHT_URL = "right_url";

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            if (timeText != null) {
                String now = new SimpleDateFormat("HH:mm:ss  •  dd/MM/yyyy", Locale.getDefault()).format(new Date());
                timeText.setText(now);
            }
            if (routePreviewView != null) routePreviewView.tick();
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadPrefs();

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            getWindow().setStatusBarColor(Color.rgb(10, 14, 19));
            getWindow().setNavigationBarColor(Color.rgb(10, 14, 19));
        } catch (Throwable ignored) {}

        buildUi();
        handler.post(clockRunnable);
        updateStatus("Sẵn sàng. Bấm GPS để bắt đầu.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        stopLocationUpdates();
    }

    private void loadPrefs() {
        dashboardVisible = prefs.getBoolean(PREF_DASHBOARD_VISIBLE, true);
        centerVisible = prefs.getBoolean(PREF_CENTER_VISIBLE, true);
        rightVisible = prefs.getBoolean(PREF_RIGHT_VISIBLE, true);
        dashboardWeight = prefs.getFloat(PREF_DASHBOARD_WEIGHT, 25f);
        centerWeight = prefs.getFloat(PREF_CENTER_WEIGHT, 45f);
        rightWeight = prefs.getFloat(PREF_RIGHT_WEIGHT, 30f);
        if (TextUtils.isEmpty(prefs.getString(PREF_CENTER_URL, ""))) {
            prefs.edit()
                    .putString(PREF_CENTER_URL, "https://maps.vietmap.vn/")
                    .putString(PREF_RIGHT_URL, "https://m.youtube.com/")
                    .apply();
        }
    }

    private void saveWeights() {
        prefs.edit()
                .putBoolean(PREF_DASHBOARD_VISIBLE, dashboardVisible)
                .putBoolean(PREF_CENTER_VISIBLE, centerVisible)
                .putBoolean(PREF_RIGHT_VISIBLE, rightVisible)
                .putFloat(PREF_DASHBOARD_WEIGHT, dashboardWeight)
                .putFloat(PREF_CENTER_WEIGHT, centerWeight)
                .putFloat(PREF_RIGHT_WEIGHT, rightWeight)
                .apply();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(buildBackground());
        root.setPadding(dp(10), dp(10), dp(10), dp(10));

        toolbar = buildToolbar();
        root.addView(toolbar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        contentRow = new LinearLayout(this);
        contentRow.setOrientation(LinearLayout.HORIZONTAL);
        contentRow.setPadding(0, dp(8), 0, 0);
        root.addView(contentRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        dashboardPanel = createPanel();
        centerPanel = createPanel();
        rightPanel = createPanel();

        buildDashboardPanel(dashboardPanel);
        buildAppSlot(centerPanel, true);
        buildAppSlot(rightPanel, false);

        applyLayoutWeights();
        setContentView(root);
    }

    private GradientDrawable buildBackground() {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(8, 12, 18), Color.rgb(16, 24, 35), Color.rgb(12, 16, 22)}
        );
        gd.setCornerRadius(0);
        return gd;
    }

    private LinearLayout buildToolbar() {
        HorizontalScrollView hsv = new HorizontalScrollView(this);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setFillViewport(true);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(8), dp(10), dp(8));
        row.setBackground(chipBg(Color.argb(200, 19, 29, 41), 16, Color.argb(80, 255, 255, 255)));
        hsv.addView(row, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        Button dashToggle = tinyButton(visibilityLabel("xe", dashboardVisible));
        dashToggle.setOnClickListener(v -> {
            if (!canToggleTo(dashboardVisible, centerVisible, rightVisible)) return;
            dashboardVisible = !dashboardVisible;
            dashToggle.setText(visibilityLabel("xe", dashboardVisible));
            applyLayoutWeights();
        });
        row.addView(dashToggle);

        row.addView(space(6));
        Button centerToggle = tinyButton(visibilityLabel("giữa", centerVisible));
        centerToggle.setOnClickListener(v -> {
            if (!canToggleTo(centerVisible, dashboardVisible, rightVisible)) return;
            centerVisible = !centerVisible;
            centerToggle.setText(visibilityLabel("giữa", centerVisible));
            applyLayoutWeights();
        });
        row.addView(centerToggle);

        row.addView(space(6));
        Button rightToggle = tinyButton(visibilityLabel("phải", rightVisible));
        rightToggle.setOnClickListener(v -> {
            if (!canToggleTo(rightVisible, dashboardVisible, centerVisible)) return;
            rightVisible = !rightVisible;
            rightToggle.setText(visibilityLabel("phải", rightVisible));
            applyLayoutWeights();
        });
        row.addView(rightToggle);

        row.addView(space(8));
        vehicleSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, VINFAST_MODELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vehicleSpinner.setAdapter(adapter);
        String selected = prefs.getString(PREF_VEHICLE, VINFAST_MODELS[0]);
        for (int i = 0; i < VINFAST_MODELS.length; i++) {
            if (VINFAST_MODELS[i].equals(selected)) {
                vehicleSpinner.setSelection(i);
                break;
            }
        }
        vehicleSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener() {
            @Override
            public void onItemSelected(int position) {
                String item = VINFAST_MODELS[position];
                prefs.edit().putString(PREF_VEHICLE, item).apply();
                if (vehicleNameText != null) vehicleNameText.setText(item + "  •  Dashboard");
                if (routePreviewView != null) routePreviewView.setVehicleLabel(item);
            }
        });
        LinearLayout.LayoutParams spLp = new LinearLayout.LayoutParams(dp(132), ViewGroup.LayoutParams.WRAP_CONTENT);
        vehicleSpinner.setLayoutParams(spLp);
        row.addView(vehicleSpinner);

        row.addView(space(10));
        row.addView(smallChip("Giữa"));
        Button cMinus = miniRoundButton("−");
        cMinus.setOnClickListener(v -> adjustWeights(true, false));
        row.addView(cMinus);
        Button cPlus = miniRoundButton("+");
        cPlus.setOnClickListener(v -> adjustWeights(true, true));
        row.addView(cPlus);

        row.addView(space(10));
        row.addView(smallChip("Phải"));
        Button rMinus = miniRoundButton("−");
        rMinus.setOnClickListener(v -> adjustWeights(false, false));
        row.addView(rMinus);
        Button rPlus = miniRoundButton("+");
        rPlus.setOnClickListener(v -> adjustWeights(false, true));
        row.addView(rPlus);

        row.addView(space(10));
        Button reset = tinyButton("Reset khung");
        reset.setOnClickListener(v -> {
            dashboardVisible = true;
            centerVisible = true;
            rightVisible = true;
            dashboardWeight = 25f;
            centerWeight = 45f;
            rightWeight = 30f;
            applyLayoutWeights();
            dashToggle.setText(visibilityLabel("xe", dashboardVisible));
            centerToggle.setText(visibilityLabel("giữa", centerVisible));
            rightToggle.setText(visibilityLabel("phải", rightVisible));
        });
        row.addView(reset);

        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(buildBrandCard(), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        hsvLp.topMargin = dp(8);
        wrapper.addView(hsv, hsvLp);
        return wrapper;
    }

    private void adjustWeights(boolean center, boolean increase) {
        float delta = 5f;
        if (center) {
            if (increase && rightWeight > 20f) {
                centerWeight += delta;
                rightWeight -= delta;
            } else if (!increase && centerWeight > 20f) {
                centerWeight -= delta;
                rightWeight += delta;
            }
        } else {
            if (increase && centerWeight > 20f) {
                rightWeight += delta;
                centerWeight -= delta;
            } else if (!increase && rightWeight > 20f) {
                rightWeight -= delta;
                centerWeight += delta;
            }
        }
        applyLayoutWeights();
    }

    private LinearLayout createPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(10), dp(10), dp(10), dp(10));
        panel.setBackground(chipBg(Color.argb(220, 16, 23, 31), 24, Color.argb(60, 255, 255, 255)));
        return panel;
    }

    private void applyLayoutWeights() {
        contentRow.removeAllViews();
        if (!dashboardVisible && !centerVisible && !rightVisible) {
            dashboardVisible = true;
        }
        if (dashboardVisible) contentRow.addView(dashboardPanel, slotLp(dashboardWeight));
        if (centerVisible) contentRow.addView(centerPanel, slotLp(centerWeight));
        if (rightVisible) contentRow.addView(rightPanel, slotLp(rightWeight));
        saveWeights();
    }

    private boolean canToggleTo(boolean currentVisible, boolean other1, boolean other2) {
        if (currentVisible && !other1 && !other2) {
            Toast.makeText(this, "Phải giữ ít nhất 1 màn hình hiển thị", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private String visibilityLabel(String name, boolean visible) {
        return visible ? "Ẩn " + name : "Hiện " + name;
    }

    private LinearLayout.LayoutParams slotLp(float weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        return lp;
    }


    private LinearLayout buildBrandCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(10), dp(8), dp(10), dp(8));
        card.setBackground(chipBg(Color.argb(120, 255, 255, 255), 18, Color.argb(50, 255, 255, 255)));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_kt);
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(34), dp(34));
        logoLp.setMargins(0, 0, dp(10), 0);
        card.addView(logo, logoLp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        TextView line1 = new TextView(this);
        line1.setText("Bản quyền phần mềm: Kim Ngọc Minh Trí");
        line1.setTextColor(Color.WHITE);
        line1.setTypeface(Typeface.DEFAULT_BOLD);
        line1.setTextSize(12);
        texts.addView(line1);

        TextView line2 = new TextView(this);
        line2.setText("Địa chỉ: Long Thành, Vĩnh Long");
        line2.setTextColor(Color.rgb(190, 205, 220));
        line2.setTextSize(11);
        texts.addView(line2);

        card.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private void buildDashboardPanel(LinearLayout parent) {
        vehicleNameText = title(prefs.getString(PREF_VEHICLE, VINFAST_MODELS[0]) + "  •  Dashboard");
        parent.addView(vehicleNameText);
        timeText = subText("--:--:--");
        timeText.setGravity(Gravity.CENTER_HORIZONTAL);
        parent.addView(timeText);

        routePreviewView = new RoutePreviewView(this);
        routePreviewView.setVehicleLabel(prefs.getString(PREF_VEHICLE, VINFAST_MODELS[0]));
        LinearLayout.LayoutParams routeLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(120));
        routeLp.setMargins(0, dp(8), 0, dp(8));
        parent.addView(routePreviewView, routeLp);

        speedText = new TextView(this);
        speedText.setText("0");
        speedText.setTextColor(Color.WHITE);
        speedText.setTextSize(64);
        speedText.setTypeface(Typeface.DEFAULT_BOLD);
        speedText.setGravity(Gravity.CENTER_HORIZONTAL);
        parent.addView(speedText);

        TextView unit = new TextView(this);
        unit.setText("km/h");
        unit.setTextColor(Color.rgb(205, 220, 235));
        unit.setTextSize(20);
        unit.setGravity(Gravity.CENTER_HORIZONTAL);
        parent.addView(unit);

        statusText = smallText("Trạng thái: chưa bật GPS");
        latLngText = smallText("Tọa độ: --");
        accuracyText = smallText("Độ chính xác: --");
        headingText = smallText("Hướng: --");
        parent.addView(statusText);
        parent.addView(latLngText);
        parent.addView(accuracyText);
        parent.addView(headingText);

        LinearLayout btnRow = new LinearLayout(this);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setWeightSum(2f);
        btnRow.addView(actionButton("GPS"), btnBarLp());
        btnRow.addView(actionButton("Quyền"), btnBarLp());
        parent.addView(btnRow);

        ((Button) btnRow.getChildAt(0)).setOnClickListener(v -> startLocationFlow());
        ((Button) btnRow.getChildAt(1)).setOnClickListener(v -> openAppSettings());

        TextView footer = subText("© Kim Ngọc Minh Trí • Long Thành, Vĩnh Long");
        footer.setPadding(0, dp(10), 0, 0);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        parent.addView(footer);
    }

    private void buildAppSlot(LinearLayout parent, boolean centerSlot) {
        String pkgKey = centerSlot ? PREF_CENTER_PACKAGE : PREF_RIGHT_PACKAGE;
        String labelKey = centerSlot ? PREF_CENTER_LABEL : PREF_RIGHT_LABEL;
        String urlKey = centerSlot ? PREF_CENTER_URL : PREF_RIGHT_URL;

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView chip = slotChip(prefs.getString(labelKey, centerSlot ? defaultCenterLabel() : defaultRightLabel()));
        if (centerSlot) centerAppChip = chip; else rightAppChip = chip;
        LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        top.addView(chip, chipLp);

        Button openBtn = miniPillButton("Mở");
        Button pickBtn = miniPillButton("Chọn");
        Button webBtn = miniPillButton("Web");
        top.addView(openBtn);
        top.addView(space(6));
        top.addView(pickBtn);
        top.addView(space(6));
        top.addView(webBtn);
        parent.addView(top);

        LinearLayout preview = new LinearLayout(this);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setGravity(Gravity.CENTER);
        preview.setBackground(chipBg(Color.argb(150, 255, 255, 255), 18, Color.argb(60, 255, 255, 255)));
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        previewLp.setMargins(0, dp(10), 0, dp(8));
        preview.setLayoutParams(previewLp);

        TextView icon = new TextView(this);
        icon.setText(centerSlot ? "🗺️" : "▶");
        icon.setTextSize(48);
        icon.setGravity(Gravity.CENTER);
        preview.addView(icon);

        TextView hint = new TextView(this);
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(18);
        hint.setTypeface(Typeface.DEFAULT_BOLD);
        hint.setText(prefs.getString(labelKey, centerSlot ? defaultCenterLabel() : defaultRightLabel()));
        hint.setGravity(Gravity.CENTER);
        preview.addView(hint);

        TextView note = subText("Ưu tiên mở app đã chọn. Nếu chưa có app, sẽ mở web trong cửa sổ vừa khung.");
        note.setGravity(Gravity.CENTER);
        note.setPadding(dp(18), dp(8), dp(18), dp(8));
        preview.addView(note);

        parent.addView(preview);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setWeightSum(2f);
        Button resizeSelf = actionButton(centerSlot ? "Giữa +" : "Phải +");
        Button shrinkSelf = actionButton(centerSlot ? "Giữa -" : "Phải -");
        bottom.addView(resizeSelf, btnBarLp());
        bottom.addView(shrinkSelf, btnBarLp());
        parent.addView(bottom);

        openBtn.setOnClickListener(v -> launchPreferred(centerSlot ? centerPanel : rightPanel,
                prefs.getString(pkgKey, centerSlot ? defaultCenterPackage() : defaultRightPackage()),
                prefs.getString(urlKey, centerSlot ? "https://maps.vietmap.vn/" : "https://m.youtube.com/")));
        webBtn.setOnClickListener(v -> openUrlIntoBounds(prefs.getString(urlKey, centerSlot ? "https://maps.vietmap.vn/" : "https://m.youtube.com/"), centerSlot ? centerPanel : rightPanel));
        pickBtn.setOnClickListener(v -> chooseApp(centerSlot, hint));

        resizeSelf.setOnClickListener(v -> adjustWeights(centerSlot, true));
        shrinkSelf.setOnClickListener(v -> adjustWeights(centerSlot, false));
    }

    private void chooseApp(boolean centerSlot, TextView hintView) {
        List<AppEntry> apps = queryLaunchableApps();
        if (apps.isEmpty()) {
            Toast.makeText(this, "Không lấy được danh sách ứng dụng", Toast.LENGTH_LONG).show();
            return;
        }
        CharSequence[] names = new CharSequence[apps.size()];
        for (int i = 0; i < apps.size(); i++) names[i] = apps.get(i).label;

        new AlertDialog.Builder(this)
                .setTitle(centerSlot ? "Chọn app cho khung giữa" : "Chọn app cho khung phải")
                .setItems(names, (dialog, which) -> {
                    AppEntry entry = apps.get(which);
                    String pkgKey = centerSlot ? PREF_CENTER_PACKAGE : PREF_RIGHT_PACKAGE;
                    String labelKey = centerSlot ? PREF_CENTER_LABEL : PREF_RIGHT_LABEL;
                    prefs.edit().putString(pkgKey, entry.packageName).putString(labelKey, entry.label).apply();
                    if (centerSlot && centerAppChip != null) centerAppChip.setText(entry.label);
                    if (!centerSlot && rightAppChip != null) rightAppChip.setText(entry.label);
                    if (hintView != null) hintView.setText(entry.label);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private List<AppEntry> queryLaunchableApps() {
        List<AppEntry> result = new ArrayList<>();
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> infos = getPackageManager().queryIntentActivities(intent, 0);
            for (ResolveInfo ri : infos) {
                if (ri.activityInfo == null || ri.activityInfo.packageName == null) continue;
                CharSequence labelCs = ri.loadLabel(getPackageManager());
                String label = labelCs == null ? ri.activityInfo.packageName : labelCs.toString();
                result.add(new AppEntry(label, ri.activityInfo.packageName));
            }
            Collections.sort(result, Comparator.comparing(a -> a.label.toLowerCase(Locale.getDefault())));
        } catch (Throwable ignored) {}
        return result;
    }

    private void launchPreferred(View boundsView, String packageName, String fallbackUrl) {
        if (!TextUtils.isEmpty(packageName)) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
            if (launch != null) {
                try {
                    launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    launchIntoBounds(launch, boundsView);
                    return;
                } catch (Throwable ignored) {}
            }
        }
        openUrlIntoBounds(fallbackUrl, boundsView);
    }

    private void openUrlIntoBounds(String url, View boundsView) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            launchIntoBounds(intent, boundsView);
        } catch (Throwable t) {
            Toast.makeText(this, "Không mở được: " + url, Toast.LENGTH_LONG).show();
        }
    }

    private void launchIntoBounds(Intent intent, View view) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 24 && view != null) {
                Rect bounds = getViewBoundsOnScreen(view);
                ActivityOptions options = ActivityOptions.makeBasic();
                options.setLaunchBounds(bounds);
                startActivity(intent, options.toBundle());
            } else {
                startActivity(intent);
            }
        } catch (Throwable t) {
            startActivity(intent);
        }
    }

    private Rect getViewBoundsOnScreen(View view) {
        int[] loc = new int[2];
        view.getLocationOnScreen(loc);
        return new Rect(loc[0], loc[1], loc[0] + view.getWidth(), loc[1] + view.getHeight());
    }

    private String defaultCenterPackage() {
        String[] packages = new String[]{"vn.vietmap.vietmap", "vn.vietmap.android", "com.vietmap", "com.google.android.apps.maps"};
        for (String pkg : packages) if (getPackageManager().getLaunchIntentForPackage(pkg) != null) return pkg;
        return "";
    }

    private String defaultCenterLabel() {
        String pkg = defaultCenterPackage();
        return resolveLabel(pkg, "Bản đồ");
    }

    private String defaultRightPackage() {
        String[] packages = new String[]{"com.google.android.youtube", "com.sec.android.app.sbrowser", "com.android.chrome"};
        for (String pkg : packages) if (getPackageManager().getLaunchIntentForPackage(pkg) != null) return pkg;
        return "";
    }

    private String defaultRightLabel() {
        String pkg = defaultRightPackage();
        return resolveLabel(pkg, "YouTube / Trình duyệt");
    }

    private String resolveLabel(String packageName, String fallback) {
        if (TextUtils.isEmpty(packageName)) return fallback;
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(packageName, 0);
            CharSequence label = getPackageManager().getApplicationLabel(ai);
            return label == null ? fallback : label.toString();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private GradientDrawable chipBg(int color, int radiusDp, int strokeColor) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(dp(radiusDp));
        gd.setStroke(dp(1), strokeColor);
        return gd;
    }

    private TextView title(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(22);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        return tv;
    }

    private TextView subText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.rgb(188, 203, 220));
        tv.setTextSize(13);
        return tv;
    }

    private TextView smallText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.rgb(205, 218, 230));
        tv.setTextSize(13);
        tv.setPadding(0, dp(3), 0, dp(3));
        return tv;
    }

    private TextView slotChip(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        tv.setPadding(dp(10), dp(6), dp(10), dp(6));
        tv.setBackground(chipBg(Color.argb(160, 255, 255, 255), 999, Color.argb(70, 255, 255, 255)));
        return tv;
    }

    private TextView smallChip(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(12);
        tv.setPadding(dp(8), dp(4), dp(8), dp(4));
        tv.setBackground(chipBg(Color.argb(100, 255, 255, 255), 999, Color.argb(60, 255, 255, 255)));
        return tv;
    }

    private Button tinyButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(12), dp(6), dp(12), dp(6));
        return b;
    }

    private Button miniRoundButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(38), dp(34));
        lp.setMargins(dp(4), 0, 0, 0);
        b.setLayoutParams(lp);
        return b;
    }

    private Button miniPillButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(12);
        b.setMinHeight(0);
        b.setMinimumHeight(0);
        b.setPadding(dp(10), dp(6), dp(10), dp(6));
        return b;
    }

    private Button actionButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(13);
        return b;
    }

    private LinearLayout.LayoutParams btnBarLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
        lp.setMargins(dp(4), dp(8), dp(4), 0);
        return lp;
    }

    private View space(int dp) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(dp), 1));
        return v;
    }

    private void startLocationFlow() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        startLocationUpdates();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                updateStatus("Chưa được cấp quyền vị trí.");
                Toast.makeText(this, "Cần quyền vị trí để đo tốc độ GPS", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startLocationUpdates() {
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (locationManager == null) {
                updateStatus("Không lấy được LocationManager.");
                return;
            }
            boolean gpsEnabled = false;
            boolean networkEnabled = false;
            try { gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (Throwable ignored) {}
            try { networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER); } catch (Throwable ignored) {}
            if (!gpsEnabled && !networkEnabled) {
                updateStatus("GPS đang tắt. Hãy bật Vị trí trên điện thoại.");
                openLocationSettings();
                return;
            }
            if (gpsEnabled) locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this);
            if (networkEnabled) locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1500, 0, this);
            updateStatus("GPS đang chạy...");
        } catch (SecurityException se) {
            updateStatus("Thiếu quyền vị trí.");
        } catch (Throwable t) {
            updateStatus("Lỗi GPS: " + safeMessage(t));
        }
    }

    private void stopLocationUpdates() {
        try {
            if (locationManager != null) locationManager.removeUpdates(this);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) return;
        float speedKmh = 0f;
        if (location.hasSpeed()) {
            speedKmh = location.getSpeed() * 3.6f;
        } else if (lastLocation != null && location.getTime() > lastLocation.getTime()) {
            float meters = lastLocation.distanceTo(location);
            float seconds = (location.getTime() - lastLocation.getTime()) / 1000f;
            if (seconds > 0) speedKmh = (meters / seconds) * 3.6f;
        }
        lastLocation = location;

        speedText.setText(String.format(Locale.getDefault(), "%.0f", speedKmh));
        latLngText.setText(String.format(Locale.US, "Tọa độ: %.6f, %.6f", location.getLatitude(), location.getLongitude()));
        accuracyText.setText(location.hasAccuracy() ? String.format(Locale.getDefault(), "Độ chính xác: %.0f m", location.getAccuracy()) : "Độ chính xác: --");
        headingText.setText(location.hasBearing() ? String.format(Locale.getDefault(), "Hướng: %.0f°", location.getBearing()) : "Hướng: --");
        updateStatus("Đang nhận tín hiệu GPS");
        if (routePreviewView != null) {
            routePreviewView.setBearing(location.hasBearing() ? location.getBearing() : 0f);
            routePreviewView.setSpeed(speedKmh);
        }
    }

    @Override public void onProviderEnabled(String provider) { updateStatus(provider + " đã bật"); }
    @Override public void onProviderDisabled(String provider) { updateStatus(provider + " đã tắt"); }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }

    private void openLocationSettings() {
        try { startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); } catch (Throwable ignored) {}
    }

    private void openAppSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } catch (Throwable t) {
            Toast.makeText(this, "Không mở được cài đặt ứng dụng", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateStatus(String s) {
        if (statusText != null) statusText.setText("Trạng thái: " + s);
    }

    private String safeMessage(Throwable t) {
        String m = t.getMessage();
        return m == null ? t.getClass().getSimpleName() : m;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class AppEntry {
        final String label;
        final String packageName;
        AppEntry(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    private abstract static class SimpleItemSelectedListener implements android.widget.AdapterView.OnItemSelectedListener {
        public abstract void onItemSelected(int position);
        @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { onItemSelected(position); }
        @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
    }

    public static class RoutePreviewView extends View {
        private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dashPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint carPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint wheelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float bearing = 0f;
        private float speed = 0f;
        private float progress = 0.15f;
        private String vehicleLabel = "VF 3";

        public RoutePreviewView(Context context) {
            super(context);
            routePaint.setColor(Color.argb(120, 70, 140, 230));
            routePaint.setStrokeWidth(dp(context, 8));
            routePaint.setStyle(Paint.Style.STROKE);
            routePaint.setStrokeCap(Paint.Cap.ROUND);

            dashPaint.setColor(Color.argb(110, 255, 255, 255));
            dashPaint.setStrokeWidth(dp(context, 2));
            dashPaint.setStyle(Paint.Style.STROKE);
            dashPaint.setStrokeCap(Paint.Cap.ROUND);

            carPaint.setColor(Color.WHITE);
            carPaint.setStyle(Paint.Style.FILL);
            wheelPaint.setColor(Color.rgb(55, 65, 75));
            wheelPaint.setStyle(Paint.Style.FILL);
            textPaint.setColor(Color.rgb(220, 235, 255));
            textPaint.setTextSize(dp(context, 11));
        }

        void setBearing(float bearing) {
            this.bearing = bearing;
            invalidate();
        }

        void setSpeed(float speed) {
            this.speed = speed;
            invalidate();
        }

        void setVehicleLabel(String label) {
            this.vehicleLabel = label;
            invalidate();
        }

        void tick() {
            float delta = speed <= 0 ? 0.005f : Math.min(0.04f, speed / 700f);
            progress += delta;
            if (progress > 0.9f) progress = 0.12f;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth();
            int h = getHeight();
            float left = dp(getContext(), 18);
            float right = w - dp(getContext(), 18);
            float top = h * 0.22f;
            float bottom = h * 0.80f;

            Path path = new Path();
            path.moveTo(left, bottom);
            path.cubicTo(w * 0.30f, h * 0.68f, w * 0.42f, h * 0.42f, w * 0.58f, h * 0.50f);
            path.cubicTo(w * 0.70f, h * 0.56f, w * 0.74f, h * 0.30f, right, top);
            canvas.drawPath(path, routePaint);

            for (int i = 0; i < 8; i++) {
                float x = left + (right - left) * (i / 7f);
                canvas.drawCircle(x, h * 0.5f, dp(getContext(), 1), dashPaint);
            }

            float x = left + (right - left) * progress;
            float y = (float) (bottom - (bottom - top) * progress * 0.9f + Math.sin(progress * 3.14f * 2) * dp(getContext(), 8));

            canvas.save();
            canvas.translate(x, y);
            canvas.rotate(bearing == 0f ? -20f : bearing);
            float carW = dp(getContext(), 34);
            float carH = dp(getContext(), 18);
            canvas.drawRoundRect(-carW / 2, -carH / 2, carW / 2, carH / 2, dp(getContext(), 6), dp(getContext(), 6), carPaint);
            canvas.drawRoundRect(-carW / 4, -carH / 2 - dp(getContext(), 2), carW / 4, carH / 2 + dp(getContext(), 2), dp(getContext(), 5), dp(getContext(), 5), carPaint);
            canvas.drawCircle(-carW / 3, -carH / 2, dp(getContext(), 3), wheelPaint);
            canvas.drawCircle(carW / 3, -carH / 2, dp(getContext(), 3), wheelPaint);
            canvas.drawCircle(-carW / 3, carH / 2, dp(getContext(), 3), wheelPaint);
            canvas.drawCircle(carW / 3, carH / 2, dp(getContext(), 3), wheelPaint);
            canvas.restore();

            canvas.drawText(vehicleLabel + "  •  xe trắng", left, h - dp(getContext(), 12), textPaint);
        }

        private static int dp(Context context, int value) {
            return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
        }
    }
}
