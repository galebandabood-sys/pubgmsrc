# NOVA — فحص تشغيل النسخة الأصلية (بدون التلاعب باللعبة)

## شنو تغيّر؟
ملفات `app/` الأصلية كانت تُنتج APK لكن لا تحتوي ملف Java `MainActivity` المطلوب داخل `AndroidManifest.xml`.
أضفنا `app/src/main/java/com/RIYAZSRC/MainActivity.java` حتى تفتح شاشة تشخيص واضحة على جهاز أندرويد، مع زر يفحص إذا مكتبة `libItsMyHubS.so` الأصلية موجودة في نفس APK.

أزلنا من **هذه النسخة التشخيصية فقط** أذونات الموقع والاتصالات والـVPN والملفات والشاشة العائمة وغير ذلك؛ ما تتطلب صلاحيات حساسة، ولا تعمل حقناً أو تحميل `System.loadLibrary` للمكتبة الأصلية لأنها تعتمد على عملية ببجي.

تم تحديث GitHub Actions ليفحص ثلاثة أمور بعد نجاح بناء المشروع:
1. تسجيل `MainActivity` كـ launcher في Manifest.
2. وجود الكلاس في `classes.dex` داخل ملف APK.
3. وجود المكتبة `lib/arm64-v8a/libItsMyHubS.so`، التي خرجت من ترجمة C++ الأصلية.

## شنو أثبتنا وشنو ما أثبتنا؟
إذا نجح CI، فهذا يثبت أن ملف APK له نقطة تشغيل صحيحة ومكتبة native مضمنة. لكنه **لا يثبت اختبار تشغيل فعلي على جهاز أندرويد، ولا ظهور قائمة ImGui داخل لعبة PUBG**.

**لا يوجد أي وعد بدعم آخر تحديث ببجي أو ميزات الهاك.**
الشفرة الأصلية ما زالت تعتمد على بنى بيانات قديمة، ويتضمن `Main.cpp` منطق هوكات اللعبة. ملف `WOLTAJ/hook.cpp` مستبعد من هذا الفرع لتشخيص قابلية البناء؛ ميزة تعطيل الحماية فيه غير موجودة هنا.

## للتجربة
انزل APK فقط من تشغيل GitHub Actions الناجح في فرع `nova-native-ui-diagnostic`، وافتح تطبيق `NOVA Native Diagnostics` واضغط زر **فحص ملفات النسخة الأصلية**. ظهور نتيجة إيجابية يدل على تغليف المكتبة، وليس تشغيلها داخل اللعبة.

يمكن فتح التغييرات: https://github.com/galebandabood-sys/pubgmsrc/pull/4

## نتيجة البناء المؤكدة — 14 نوفمبر 2026

- GitHub Actions: https://github.com/galebandabood-sys/pubgmsrc/actions/runs/43614714366
- مهمة `assembleDebug`: ناجحة.
- مهمة التحقق من `com.RIYAZSRC.MainActivity` و`classes.dex` و`libItsMyHubS.so`: ناجحة.
- اسم الـArtifact: `NOVA-original-launcher-diagnostic-apk`، يحتوي `app-debug.apk`.
- تم تنزيل الـArtifact وفحص APK محلياً: `classes.dex` يحتوي الكلاس `Lcom/RIYAZSRC/MainActivity;` والمكتبة `lib/arm64-v8a/libItsMyHubS.so` موجودة.
- حجم `app-debug.apk`: 6,417,662 بايت.
- فحص SHA256: `8ed8c3e8b7c798d500a9bf3c82033633fbef5e39bad6d2a6c7ddd1d09273aee8`.
- **لم يتم تشغيل النسخة على جهاز أندرويد فعلي. ولم يتم تشغيل المنيو الأصلي داخل PUBG.**
- شاشة الـActivity الجديدة *تشخيص* لوجود مكتبة C++، وليست منيو غش شغّال أو تجاوز مكافحة الغش.
