package com.RIYAZSRC;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Premium-looking, honest control center for the standalone native game overlay. */
public final class MainActivity extends Activity {
    private static final int BASE = 0xff0e091a;
    private static final int PANEL = 0xff1b122c;
    private static final int PANEL_DARK = 0xff110c20;
    private static final int PURPLE = 0xff965ff7;
    private static final int BORDER = 0xff4a386a;
    private static final int WHITE = 0xfff5eeff;
    private static final int MUTED = 0xffb7a9ce;
    private static final String PREFS = "nova_prime";

    private LinearLayout content;
    private TextView monitorStatus;
    private TextView libraryStatus;

    private int dp(float d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable rounded(int fill, int border, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(fill);
        shape.setCornerRadius(dp(radius));
        if (border != 0) shape.setStroke(dp(1), border);
        return shape;
    }

    private GradientDrawable gradient(int start, int end, int radius) {
        GradientDrawable shape = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR, new int[]{start, end});
        shape.setCornerRadius(dp(radius));
        return shape;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(size);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(21), dp(20), dp(20));
        card.setBackground(rounded(PANEL, BORDER, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                -1, -2);
        params.bottomMargin = dp(16);
        content.addView(card, params);
        return card;
    }

    private void addText(LinearLayout parent, String content, int sp,
            int color, boolean bold, int spacing) {
        TextView view = label(content, sp, color, bold);
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(spacing);
        parent.addView(view, params);
    }

    private Button primaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setBackground(gradient(0xff9b59fa, 0xff7947df, 16));
        return button;
    }

    private Button secondaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(WHITE);
        button.setBackground(rounded(0xff342a46, 0xff504565, 16));
        return button;
    }

    private void addButton(LinearLayout parent, Button button, int top) {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, dp(54));
        params.topMargin = dp(top);
        parent.addView(button, params);
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BASE);
        getWindow().setNavigationBarColor(BASE);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xff25133b, BASE, 0xff1c102d}));
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setPadding(dp(20), dp(32), dp(20), dp(32));
        scroll.addView(content);
        setContentView(scroll);

        TextView brand = label("NOVA PRIME", 35, WHITE, true);
        brand.setTextDirection(View.TEXT_DIRECTION_LTR);
        brand.setGravity(Gravity.CENTER);
        content.addView(brand);
        TextView subtitle = label("MANUAL OVERLAY HUD   •   5.2 / PUBG 4.7",
                12, 0xffb99bea, false);
        subtitle.setTextDirection(View.TEXT_DIRECTION_LTR);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.bottomMargin = dp(27);
        content.addView(subtitle, subParams);

        LinearLayout hero = card();
        addText(hero, "إصدار المطوّر  /  ARM64", 13, 0xffc9b3f3, false, 0);
        addText(hero, "مكتبة العرض التجريبية", 23, WHITE, true, 6);
        addText(hero, "منيو Dear ImGui حقيقي للعرض فوق اللعبة؛ المكتبة القديمة موجودة للتشخيص فقط وغير محقونة في PUBG.",
                14, MUTED, false, 13);
        LinearLayout fileChip = new LinearLayout(this);
        fileChip.setPadding(dp(13), dp(14), dp(13), dp(14));
        fileChip.setOrientation(LinearLayout.VERTICAL);
        fileChip.setBackground(rounded(PANEL_DARK, 0, 15));
        LinearLayout.LayoutParams fileParams =
                new LinearLayout.LayoutParams(-1, -2);
        fileParams.topMargin = dp(16);
        hero.addView(fileChip, fileParams);
        addText(fileChip, "libnova_preview.so  •  arm64-v8a", 16, WHITE, true, 0);
        libraryStatus = label("فحص مكتبة C++...", 13, MUTED, false);
        fileChip.addView(libraryStatus);
        Button checkLibrary = secondaryButton("فحص المكتبة داخل التطبيق");
        addButton(hero, checkLibrary, 15);
        checkLibrary.setOnClickListener(v -> updateLibraryStatus());
        Button preview = secondaryButton("تجربة المنيو بدون فتح PUBG");
        addButton(hero, preview, 10);
        preview.setOnClickListener(v -> startActivity(
                new Intent(this, NativePreviewActivity.class)));


        LinearLayout monitor = card();
        addText(monitor, "المنيو اليدوي بدون وصول الاستخدام", 22, WHITE, true, 0);
        addText(monitor,
                "ما يحتاج إذن مراقبة التطبيقات ولا نسخ OBB. لتظهر فقاعة N فوق PUBG لازم فقط إذن الظهور فوق التطبيقات. الفقاعة تبقى ظاهرة إلى أن توقفها يدوياً.",
                14, MUTED, false, 10);
        monitorStatus = label("جاري فحص إذن الظهور...", 14, 0xffdcc7ff, true);
        LinearLayout.LayoutParams statusParams =
                new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(12);
        monitor.addView(monitorStatus, statusParams);
        Button overlayGrant = secondaryButton("1  •  إذن الظهور فوق التطبيقات فقط");
        addButton(monitor, overlayGrant, 14);
        overlayGrant.setOnClickListener(v -> requestOverlayPermission());

        Button prepare = primaryButton("2  •  تشغيل فقاعة N يدوياً");
        addButton(monitor, prepare, 16);
        prepare.setOnClickListener(v -> startMonitoring());
        Button stop = secondaryButton("إيقاف الفقاعة وإخفاء المنيو");
        addButton(monitor, stop, 10);
        stop.setOnClickListener(v -> {
            boolean stopped = stopService(new Intent(this, NativeOverlayService.class));
            Toast.makeText(this, stopped ? "تم إيقاف الفقاعة والمنيو" :
                    "الفقاعة متوقفة", Toast.LENGTH_SHORT).show();
        });

        addText(content, "الألعاب المتاحة للفتح", 23, WHITE, true, 7);
        addText(content, "الإصدارات المعروضة مأخوذة من الألعاب المثبّتة على جهازك.",
                13, MUTED, false, 6);
        int index = 0;
        for (String[] game : GameRuntime.GAMES) {
            boolean installed = getPackageManager().getLaunchIntentForPackage(game[1]) != null;
            if (installed || index < 2) addGameCard(game[0], game[1], installed);
            index++;
        }

        LinearLayout available = card();
        addText(available, "الميزات المرتبطة بالمنيو", 18, WHITE, true, 0);
        addText(available, "المتوفر حالياً: علامة مرئية ثابتة، دائرة إرشادية، وإعدادات شكل المنيو فقط. أزلنا الساعة والمؤقت وعداد FPS.",
                14, 0xff8ce6bf, false, 12);
        addText(available,
                "ESP والإيم وNo Recoil وMagic Bullet غير مفعّلة، لأن إزاحات SDK القديم غير متحققة لببجي 4.7. لن نعرضها كمفاتيح فعّالة وهمية.",
                13, 0xffffc88b, false, 12);
        addText(available, "لا روت ولا وصول الاستخدام ولا OBB. بعد خروجك من اللعبة، أوقف الفقاعة من الإشعار أو NOVA.", 12, MUTED, false, 12);
        updateLibraryStatus();
        updateMonitorStatus();
    }

    private void addGameCard(String title, String pkg, boolean installed) {
        LinearLayout card = card();
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(heading);

        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconParams =
                new LinearLayout.LayoutParams(dp(54), dp(54));
        iconParams.leftMargin = dp(13);
        heading.addView(icon, iconParams);
        try {
            icon.setImageDrawable(getPackageManager().getApplicationIcon(pkg));
        } catch (PackageManager.NameNotFoundException missing) {
            icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        heading.addView(labels,
                new LinearLayout.LayoutParams(0, -2, 1.f));
        addText(labels, title, 18, WHITE, true, 0);
        addText(labels, pkg, 12, MUTED, false, 4);

        String version = "غير مثبتة";
        String buildNote = "ملفات اللعبة غير مفحوصة";
        if (installed) {
            try {
                PackageInfo info = getPackageManager().getPackageInfo(pkg, 0);
                version = "إصدار اللعبة: " + info.versionName +
                        "  |  البناء: " + info.versionCode;
                if ("com.tencent.ig".equals(pkg) &&
                        "4.7.0".equals(info.versionName) && info.versionCode == 21740) {
                    buildNote = "مطابقة رقم النسخة المفحوصة من جهازك • واجهة HUD فقط، لا يعني توافق إزاحات الذاكرة.";
                } else {
                    buildNote = "الإصدار مختلف عن النسخة 4.7.0 (21740) المسجلة؛ المنيو المرئي فقط.";
                }
            } catch (PackageManager.NameNotFoundException ignored) {
                buildNote = "تعذّر قراءة رقم البناء";
            }
        }
        addText(card, version, 14, installed ? 0xffc4a7ff : MUTED, false, 12);
        if (installed) addText(card, buildNote, 12, MUTED, false, 6);
        Button launch = installed ? primaryButton("تشغيل الفقاعة وفتح اللعبة")
                : secondaryButton("اللعبة غير مثبتة");
        launch.setEnabled(installed);
        addButton(card, launch, 15);
        launch.setOnClickListener(v -> {
            if (!startMonitoring()) return;
            Intent intent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent == null) {
                Toast.makeText(this, "تعذّر العثور على اللعبة", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                startActivity(intent);
            } catch (RuntimeException error) {
                Toast.makeText(this, "تعذّر تشغيل اللعبة", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean startMonitoring() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return false;
        }
        try {
            Intent intent = new Intent(this, NativeOverlayService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
            else startService(intent);
            Toast.makeText(this, "الفقاعة N شغالة يدوياً — أوقفها بعد اللعب",
                    Toast.LENGTH_LONG).show();
            return true;
        } catch (RuntimeException error) {
            Toast.makeText(this, "تعذّر تشغيل المراقبة: " +
                    error.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private void requestOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "الإذن مفعّل", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException error) {
            Toast.makeText(this, "افتح إعدادات التطبيقات وفعّل الظهور فوق التطبيقات",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void updateLibraryStatus() {
        if (libraryStatus == null) return;
        try (ZipFile z = new ZipFile(getApplicationInfo().sourceDir)) {
            ZipEntry nativeLib = z.getEntry("lib/arm64-v8a/libItsMyHubS.so");
            ZipEntry renderer = z.getEntry("lib/arm64-v8a/libnova_preview.so");
            if (renderer != null) {
                double size = renderer.getSize() / (1024.0 * 1024.0);
                libraryStatus.setText(String.format(Locale.US,
                        "● منيو ImGui مدمج • %.1f MB | المكتبة القديمة: %s (غير مفعلة)",
                        size, nativeLib != null ? "موجودة" : "غير موجودة"));
                libraryStatus.setTextColor(0xff63dfb4);
            } else {
                libraryStatus.setText("ملف عرض المنيو ImGui ناقص");
                libraryStatus.setTextColor(0xffffbb91);
            }
        } catch (IOException failure) {
            libraryStatus.setText("ما تمكنت من فحص مكتبات الـAPK");
            libraryStatus.setTextColor(0xffffbb91);
        }
    }


    private void updateMonitorStatus() {
        if (monitorStatus == null) return;
        if (Settings.canDrawOverlays(this)) {
            monitorStatus.setText("● إذن الظهور مفعّل • جاهز لتشغيل الفقاعة يدوياً");
            monitorStatus.setTextColor(0xff63dfb4);
        } else {
            monitorStatus.setText("● يحتاج إذن الظهور فوق التطبيقات فقط");
            monitorStatus.setTextColor(0xffffc88b);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        updateMonitorStatus();
    }
}
