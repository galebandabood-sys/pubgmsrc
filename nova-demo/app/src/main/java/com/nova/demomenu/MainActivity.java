package com.nova.demomenu;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(11, 15, 28);
    private static final int CARD = Color.rgb(20, 27, 46);
    private static final int BORDER = Color.rgb(44, 56, 79);
    private static final int WHITE = Color.rgb(241, 245, 255);
    private static final int MUTED = Color.rgb(160, 176, 203);
    private static final int[] ACCENTS = {
            Color.rgb(130, 105, 255), Color.rgb(22, 194, 220),
            Color.rgb(39, 203, 155), Color.rgb(255, 167, 77)
    };
    private static final String[] ACCENT_NAMES = {"بنفسجي", "سماوي", "أخضر", "برتقالي"};
    private static final String[] TABS = {"الرئيسية", "المظهر", "الخصوصية", "حول"};

    private SharedPreferences preferences;
    private LinearLayout root;
    private LinearLayout panels;
    private int currentTab;
    private int accentIndex;
    private boolean compact;
    private boolean largeText;
    private boolean screenSecure;
    private boolean keepAwake;
    private boolean rememberTab;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        preferences = getSharedPreferences("nova_ui", MODE_PRIVATE);
        accentIndex = preferences.getInt("accent", 0) % ACCENTS.length;
        if (accentIndex < 0) accentIndex = 0;
        compact = preferences.getBoolean("compact", false);
        largeText = preferences.getBoolean("large_text", false);
        screenSecure = preferences.getBoolean("screen_secure", false);
        keepAwake = preferences.getBoolean("keep_awake", false);
        rememberTab = preferences.getBoolean("remember_tab", true);
        currentTab = rememberTab ? preferences.getInt("tab", 0) : 0;
        if (currentTab < 0 || currentTab >= TABS.length) currentTab = 0;
        applyWindowFlags();
        render();
    }

    private void applyWindowFlags() {
        if (screenSecure) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        if (keepAwake) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + .5f);
    }

    private GradientDrawable bg(int fill, int border, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (border != 0) shape.setStroke(dp(1), border);
        return shape;
    }

    private TextView label(String value, int size, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size + (largeText ? 2 : 0));
        text.setTextColor(color);
        text.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
        text.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private LinearLayout vertical() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        return box;
    }

    private void pad(View v, int h, int ver) {
        v.setPadding(dp(h), dp(ver), dp(h), dp(ver));
    }

    private void render() {
        root = vertical();
        root.setBackgroundColor(BACKGROUND);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout heading = vertical();
        pad(heading, 22, 17);
        TextView brand = label("NOVA  /  نوفا", 26, WHITE, true);
        TextView sub = label("لوحة تحكم عربية + فقاعة عائمة فوق التطبيقات", 12, MUTED, false);
        heading.addView(brand);
        heading.addView(sub);
        root.addView(heading);

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabs.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tabs.setPadding(dp(12), 0, dp(12), dp(8));
        for (int i = 0; i < TABS.length; i++) {
            final int tab = i;
            Button b = new Button(this);
            b.setText(TABS[i]);
            b.setTextSize(11);
            b.setAllCaps(false);
            b.setTextColor(i == currentTab ? WHITE : MUTED);
            b.setBackground(bg(i == currentTab ? ACCENTS[accentIndex] : CARD, BORDER, 12));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(44), 1f);
            lp.setMargins(dp(3), 0, dp(3), 0);
            tabs.addView(b, lp);
            b.setOnClickListener(v -> {
                currentTab = tab;
                if (rememberTab) preferences.edit().putInt("tab", tab).apply();
                render();
            });
        }
        root.addView(tabs);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        panels = vertical();
        panels.setPadding(dp(16), dp(5), dp(16), dp(16));
        if (currentTab == 0) home();
        if (currentTab == 1) appearance();
        if (currentTab == 2) privacy();
        if (currentTab == 3) about();
        scroll.addView(panels);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView footer = label("NOVA 2.0  •  منيو عائم مستقل • بدون هوكات للعبة", 11, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        pad(footer, 12, 14);
        root.addView(footer);
        setContentView(root);
    }

    private LinearLayout card(String title, String sub) {
        LinearLayout box = vertical();
        box.setBackground(bg(CARD, BORDER, 18));
        pad(box, 17, compact ? 10 : 17);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, dp(9), 0, dp(9));
        panels.addView(box, lp);
        if (title != null) {
            TextView name = label(title, 18, WHITE, true);
            box.addView(name);
        }
        if (sub != null) {
            TextView detail = label(sub, 12, MUTED, false);
            pad(detail, 0, 5);
            box.addView(detail);
        }
        return box;
    }

    private void info(LinearLayout parent, String title, String detail) {
        TextView top = label(title, 14, WHITE, true);
        pad(top, 0, 7);
        parent.addView(top);
        TextView bottom = label(detail, 12, MUTED, false);
        parent.addView(bottom);
    }

    private interface ToggleChange { void changed(boolean value); }

    private void toggle(LinearLayout parent, String title, String note,
                        boolean checked, ToggleChange change) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0, dp(10), 0, dp(8));

        LinearLayout textBox = vertical();
        textBox.addView(label(title, 14, WHITE, true));
        textBox.addView(label(note, 11, MUTED, false));
        row.addView(textBox, new LinearLayout.LayoutParams(0, -2, 1));

        Switch control = new Switch(this);
        control.setChecked(checked);
        row.addView(control);
        control.setOnCheckedChangeListener((button, enabled) -> change.changed(enabled));
        parent.addView(row);
    }

    private Button action(LinearLayout parent, String text, View.OnClickListener onClick) {
        Button b = new Button(this);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setText(text);
        b.setTextColor(WHITE);
        b.setBackground(bg(ACCENTS[accentIndex], 0, 12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
        lp.setMargins(0, dp(12), 0, dp(2));
        parent.addView(b, lp);
        b.setOnClickListener(onClick);
        return b;
    }

    private void home() {
        LinearLayout hello = card("NOVA — المنيو العائم", "افتح الفقاعة فوق التطبيقات المسموح بها");
        info(hello, "منيو حقيقي بنظام أندرويد", "الفقاعة قابلة للسحب والفتح والإغلاق. لا تتصل بذاكرة ببجي.");
        info(hello, "حالة إذن الظهور", Settings.canDrawOverlays(this) ? "مفعّل ✓" : "غير مفعّل — يلزم إذن من إعدادات أندرويد");
        action(hello, "1. السماح بالظهور فوق التطبيقات", v -> askOverlayPermission());
        action(hello, "2. تشغيل الفقاعة العائمة", v -> startFloatingMenu());
        action(hello, "3. فتح PUBG Mobile", v -> launchPubg());

        LinearLayout live = card("الميزات الموجودة وتشتغل", "كل الخيارات هذه داخل NOVA فقط");
        info(live, "الفقاعة المتحركة", "اسحب N لأي مكان، واضغط عليها حتى يفتح المنيو.");
        info(live, "ساعة ومؤقت تركيز", "مؤقت 5 دقائق + إضافة دقيقة + إيقاف، ويظهر على الفقاعة.");
        info(live, "التصميم", "تغيير الألوان والشفافية، وحفظ الإعدادات.");
        action(live, "إيقاف المنيو العائم", v -> {
            Intent stop = new Intent(this, OverlayService.class).setAction(OverlayService.ACTION_STOP);
            startService(stop);
            Toast.makeText(this, "تم إرسال أمر الإيقاف", Toast.LENGTH_SHORT).show();
        });

        LinearLayout scope = card("مهم بخصوص اللعبة", "توافق الطبقات العائمة يختلف حسب الجهاز واللعبة");
        info(scope, "لا يوجد حقن أو تعديل ذاكرة", "NOVA Overlay لا يشغل ESP أو Aim أو Bypass ولا يعدّل ملفات اللعبة.");
        info(scope, "على آخر إصدار من PUBG", "إذا سمح النظام واللعبة بالطبقات العائمة، تقدر تشوف الفقاعة فوقها. غير مضمون على جميع الأجهزة.");
    }

    private void askOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "الإذن مفعّل بالفعل", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent permission = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        try {
            startActivity(permission);
        } catch (Exception e) {
            Toast.makeText(this, "افتح إعدادات الهاتف وفعّل الظهور فوق التطبيقات لـ NOVA", Toast.LENGTH_LONG).show();
        }
    }

    private void startFloatingMenu() {
        if (!Settings.canDrawOverlays(this)) {
            askOverlayPermission();
            return;
        }
        Intent overlay = new Intent(this, OverlayService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(overlay);
            else startService(overlay);
            Toast.makeText(this, "منيو NOVA العائم شغال — اضغط فقاعة N", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "فشل تشغيل المنيو. راجع أذونات التطبيق.", Toast.LENGTH_LONG).show();
        }
    }

    private void launchPubg() {
        String[] packages = {"com.tencent.ig", "com.pubg.imobile", "com.pubg.krmobile", "com.vng.pubgmobile"};
        for (String pkg : packages) {
            try {
                Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
                if (launch != null) {
                    startActivity(launch);
                    return;
                }
            } catch (Exception ignored) { }
        }
        new AlertDialog.Builder(this)
                .setTitle("PUBG Mobile غير موجودة بالقائمة")
                .setMessage("افتح اللعبة بشكل عادي من شاشة الهاتف. إذا شغّلت NOVA قبلها، تبقى الفقاعة طالما النظام يسمح بإظهارها.")
                .setPositiveButton("تمام", null)
                .show();
    }

    @Override protected void onResume() {
        super.onResume();
        if (root != null) render();
    }

    private void appearance() {
        LinearLayout theme = card("المظهر", "خصّص المنيو على ذوقك");
        info(theme, "اللون الحالي", ACCENT_NAMES[accentIndex]);
        action(theme, "تغيير لون الواجهة", v -> {
            accentIndex = (accentIndex + 1) % ACCENTS.length;
            preferences.edit().putInt("accent", accentIndex).apply();
            render();
        });
        toggle(theme, "الوضع المدمج", "مسافات أقل بين عناصر الواجهة", compact, on -> {
            compact = on;
            preferences.edit().putBoolean("compact", on).apply();
            render();
        });
        toggle(theme, "نص أكبر", "تكبير أحجام النصوص", largeText, on -> {
            largeText = on;
            preferences.edit().putBoolean("large_text", on).apply();
            render();
        });
        toggle(theme, "تذكّر آخر صفحة", "افتح آخر تبويب استخدمته", rememberTab, on -> {
            rememberTab = on;
            preferences.edit().putBoolean("remember_tab", on).apply();
        });
    }

    private void privacy() {
        LinearLayout secure = card("الخصوصية والثبات", "هذه الخيارات تخص تطبيق NOVA فقط");
        toggle(secure, "منع لقطات الشاشة", "يستخدم FLAG_SECURE داخل هذا التطبيق", screenSecure, on -> {
            screenSecure = on;
            preferences.edit().putBoolean("screen_secure", on).apply();
            applyWindowFlags();
            Toast.makeText(this, on ? "تم تفعيل حماية الشاشة" : "تم إيقاف حماية الشاشة", Toast.LENGTH_SHORT).show();
        });
        toggle(secure, "إبقاء الشاشة شغالة", "يمنع إطفاء الشاشة أثناء فتح NOVA", keepAwake, on -> {
            keepAwake = on;
            preferences.edit().putBoolean("keep_awake", on).apply();
            applyWindowFlags();
        });

        LinearLayout reset = card("إعادة الضبط", "ترجع إعدادات الواجهة للقيم الافتراضية");
        action(reset, "إعادة ضبط الإعدادات", v -> new AlertDialog.Builder(this)
                .setTitle("إعادة ضبط NOVA")
                .setMessage("هل تريد مسح إعدادات الواجهة المحفوظة؟")
                .setNegativeButton("إلغاء", null)
                .setPositiveButton("إعادة الضبط", (dialog, which) -> {
                    preferences.edit().clear().apply();
                    recreate();
                }).show());
    }

    private void about() {
        LinearLayout details = card("عن NOVA", "إصدار 2.0 • Android floating menu");
        info(details, "نوع المشروع", "تطبيق مستقل لتجربة تصميم المنيو.");
        info(details, "التوافق", "Android 6.0 وما بعده (حسب إعدادات البناء).");
        info(details, "الحماية", "بدون صلاحيات حساسة، مع إعداد أمان للشاشة.");
        info(details, "ببجي موبايل", "يظهر فوق التطبيقات التي تسمح بذلك ولا يحقن مكتبات أو يعدّل اللعبة.");
        action(details, "رجوع للرئيسية", v -> { currentTab = 0; render(); });
    }
}
