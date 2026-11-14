# حالة إصلاح بناء مشروع PUBG الأصلي (NOVA)

هذا الفرع `nova-original-build-repair` **يعمل على المشروع الأصلي app/** وليس تطبيق المنيو التجريبي.

## إصلاحات نفّذت فعلياً
- تبديل تكوين Android Gradle من `cmake` إلى `ndkBuild` لأن `Android.mk` ليس ملف CMake.
- تصحيح كتابة `main.cpp` إلى `Main.cpp` في `Android.mk` للأجهزة Linux.
- إزالة ABI `armeabi-v7a` غير المتوافق مع `Application.mk`، والإبقاء على `arm64-v8a`.
- ضبط Build Tools إلى `31.0.0` واستخدام NDK `21.0.6113669` في GitHub Actions.
- تصحيح مسارات ترويسات ImGui في `WOLTAJ/Helper/Includes.h`.

## ماذا يعني نجاح البناء؟
نجاح `assembleDebug` يعني إنشاء تطبيق أندرويد تقنياً؛ **لا يعني** دعم آخر إصدار من PUBG، ولا يعني أن مكتبة ImGui أصلية تُحمّل داخل اللعبة تلقائياً.

## تنبيه
المصدر الأصلي يتضمن هوكات وإزاحات خاصة بإصدارات قديمة، وأوامر تمسح أو تعدل مسارات من بيانات اللعبة. لا تثبّت أي APK ناتج على جهازك الأساسي قبل تدقيق هذه الأوامر ومخاطرها. لم يتم اختبار هذه التغييرات على جهاز حقيقي.

## عرض سجل التجميع
افتح [GitHub Actions](https://github.com/galebandabood-sys/pubgmsrc/actions) واختر فحص **Original native Android build check** ثم انظر نتيجة `Compile original app (diagnostic)`.

لا يوجد ضمان أو تأكيد بتوافق PUBG Mobile الحديثة.

## آخر نتيجة فحص فعلية (14 نوفمبر 2026)
- نتيجة GitHub Actions: **فشل بناء المشروع الأصلي**.
- بعد إصلاح إعدادات Gradle، NDK، ومسارات ImGui و `xorstr.h` و `icon.h`، وصل المترجم إلى `app/src/main/jni/KeyAuth.hpp:267`.
- الخطأ: `fatal error: 'curl/curl.h' file not found`.
- هذا اعتماد خارجي ناقص (libcurl headers + مكتبة Android arm64). لا يكفي إنشاء ترويسة فارغة أو تجاهل الخطأ؛ يلزم توفير اعتماد أصلي متوافق والتحقق من ربطه وترخيصه.
- سجل البناء: https://github.com/galebandabood-sys/pubgmsrc/actions/runs/43610297972
- **لم ينتج APK أصلي صالح أو مختبر من هذا الفحص.**
