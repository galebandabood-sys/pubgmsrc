package com.RIYAZSRC;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.os.Build;
import android.widget.Toast;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A safe, launchable diagnostic shell for the repository's original Android app.
 *
 * The original native library is packaged, NOT loaded or injected. Its initialization
 * depends on another game's internal process. Launching it in this standalone APK
 * would not produce an in-game ImGui menu and may execute old unsafe native hooks.
 */
public final class MainActivity extends Activity {
    private final int canvas = Color.rgb(12, 17, 31);
    private final int panel = Color.rgb(23, 31, 50);
    private final int white = Color.rgb(239, 244, 255);
    private final int muted = Color.rgb(168, 180, 205);
    private final int purple = Color.rgb(127, 101, 240);
    private LinearLayout content;
    private TextView result;

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private GradientDrawable background(int fill, int border, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        if (border != 0) d.setStroke(dp(1), border);
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.RIGHT);
        view.setTextDirection(View.TEXT_DIRECTION_RTL);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private void addCard(String heading, String description) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackground(background(panel, Color.rgb(47, 59, 85), 16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(14);
        content.addView(card, lp);
        TextView title = text(heading, 17, white, true);
        card.addView(title);
        TextView detail = text(description, 13, muted, false);
        detail.setPadding(0, dp(9), 0, 0);
        card.addView(detail);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(canvas);
        getWindow().setNavigationBarColor(canvas);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR & 0);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(canvas);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setPadding(dp(20), dp(27), dp(20), dp(24));
        scroll.addView(content);
        setContentView(scroll);

        TextView brand = text("NOVA / فحص المنيو الأصلي", 24, white, true);
        content.addView(brand);
        TextView caption = text("Android native build diagnostic", 13, purple, false);
        caption.setPadding(0, dp(6), 0, dp(20));
        content.addView(caption);

        addCard("شنو يشتغل بهذه النسخة؟",
            "هذه شاشة تشغيل حقيقية لتطبيق app/ الأصلي. تفحص مكتبة C++ وتشغّل منيو ImGui العائم المستقل.");
        addCard("ليش المنيو ما يفتح داخل اللعبة؟",
            "فتح المنيو فوق اللعبة يعتمد على سماح أندرويد واللعبة بالطبقات العائمة. خيارات ImGui نفسها تشتغل، لكن الميزات المعتمدة على ذاكرة اللعبة تحتاج SDK صالحاً واختباراً خاصاً.");
        addCard("السلامة",
            "نطلب إذن الظهور فوق التطبيقات فقط، وتستخدم الفقاعة إشعاراً دائماً لإيقافها. لا نحمّل مكتبة الهوكات القديمة داخل المنيو العائم.");

        Button test = new Button(this);
        test.setAllCaps(false);
        test.setText("فحص ملفات النسخة الأصلية");
        test.setTextColor(white);
        test.setTextSize(15);
        test.setBackground(background(purple, 0, 12));
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-1, dp(55));
        bl.topMargin = dp(8);
        content.addView(test, bl);

        Button previewButton = new Button(this);
        previewButton.setAllCaps(false);
        previewButton.setText("فتح منيو ImGui الأصلي — تجربة رسم ولمس");
        previewButton.setTextColor(white);
        previewButton.setTextSize(15);
        previewButton.setBackground(background(Color.rgb(47, 60, 92), purple, 12));
        LinearLayout.LayoutParams pbl = new LinearLayout.LayoutParams(-1, dp(58));
        pbl.topMargin = dp(14);
        content.addView(previewButton, pbl);
        previewButton.setOnClickListener(v ->
            startActivity(new Intent(this, NativePreviewActivity.class)));
        TextView previewHint = text(
            "يفتح رسم ImGui الحقيقي بـ C++ وOpenGL ES. هذه معاينة منفصلة داخل التطبيق.",
            12, muted, false);
        previewHint.setPadding(0, dp(7), 0, dp(13));
        content.addView(previewHint);

        addCard("منيو ImGui فوق اللعبة",
            "فعّل إذن أندرويد وشغّل فقاعة N. الضغط عليها يفتح منيو C++ حقيقي فوق أي تطبيق يسمح بالطبقات العائمة. خيارات اللون والشفافية والتبويبات تعمل فعلاً، لكن ESP وAim وباقي التعديلات على ذاكرة ببجي غير متصلة.");
        Button grant = new Button(this);
        grant.setAllCaps(false);
        grant.setText("1. السماح بالظهور فوق التطبيقات");
        content.addView(grant);
        grant.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "إذن الظهور مفعّل بالفعل", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            try { startActivity(intent); }
            catch (Exception error) {
                Toast.makeText(this, "افتح إعدادات أندرويد وفعّل إذن الظهور", Toast.LENGTH_LONG).show();
            }
        });

        Button overlay = new Button(this);
        overlay.setAllCaps(false);
        overlay.setText("2. تشغيل منيو ImGui العائم");
        content.addView(overlay);
        overlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                grant.performClick();
                return;
            }
            try {
                Intent i = new Intent(this, NativeOverlayService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                else startService(i);
                Toast.makeText(this, "شغّلت فقاعة N — افتح اللعبة واضغط عليها", Toast.LENGTH_LONG).show();
            } catch (RuntimeException error) {
                Toast.makeText(this, "أندرويد منع تشغيل خدمة المنيو: " +
                        error.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            }
        });

        Button launch = new Button(this);
        launch.setAllCaps(false);
        launch.setText("3. فتح PUBG Mobile المثبّتة");
        content.addView(launch);
        launch.setOnClickListener(v -> {
            for (String pkg : new String[]{"com.tencent.ig", "com.pubg.imobile",
                    "com.pubg.krmobile", "com.vng.pubgmobile"}) {
                Intent game = getPackageManager().getLaunchIntentForPackage(pkg);
                if (game != null) {
                    startActivity(game);
                    return;
                }
            }
            Toast.makeText(this, "ما لقيت نسخة مدعومة من ببجي؛ افتحها يدوياً", Toast.LENGTH_LONG).show();
        });

        Button stopOverlay = new Button(this);
        stopOverlay.setAllCaps(false);
        stopOverlay.setText("إيقاف الفقاعة والمنيو");
        content.addView(stopOverlay);
        stopOverlay.setOnClickListener(v -> {
            boolean stopped = stopService(new Intent(this, NativeOverlayService.class));
            Toast.makeText(this, stopped ? "تم إيقاف المنيو" : "المنيو متوقف بالفعل",
                    Toast.LENGTH_SHORT).show();
        });

        result = text("اضغط الزر لفحص وجود مكتبة C++ بالـAPK.", 13, muted, false);
        result.setPadding(0, dp(16), 0, dp(12));
        content.addView(result);
        test.setOnClickListener(v -> checkPackagedLibrary());
    }

    private void checkPackagedLibrary() {
        String apk = getApplicationInfo().sourceDir;
        try (ZipFile file = new ZipFile(apk)) {
            ZipEntry entry = file.getEntry("lib/arm64-v8a/libItsMyHubS.so");
            if (entry != null) {
                result.setText("✓ ملف libItsMyHubS.so موجود داخل APK. الحجم: "
                    + entry.getSize() + " بايت. هذا يثبت التغليف فقط، مو تشغيل المنيو داخل ببجي.");
                result.setTextColor(Color.rgb(87, 221, 166));
            } else {
                result.setText("✕ مكتبة C++ غير موجودة داخل APK؛ لازم نراجع عملية البناء.");
                result.setTextColor(Color.rgb(255, 153, 136));
            }
        } catch (IOException error) {
            result.setText("تعذّر قراءة ملف APK للتشخيص: " + error.getClass().getSimpleName());
            result.setTextColor(Color.rgb(255, 153, 136));
        }
    }
}
