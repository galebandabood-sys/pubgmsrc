# NOVA — معاينة محرك ImGui الأصلي على أندرويد

هذا فرع تجريبي مبني على فرع إصلاح `app/` الأصلي، وليس تطبيق `nova-demo` السابق.

## شنو صار جديد؟
- تمت إضافة `NativePreviewActivity.java` بشاشة `GLSurfaceView` بنظام OpenGL ES 3.
- تمت إضافة `ImGui/NovaNativePreview.cpp`: رسم حقيقي من لغة C++ باستخدام ملفات `ImGui/imgui*.cpp` الموجودة بالمستودع.
- ملف `Android.mk` يبني مكتبة منفصلة باسم `libnova_preview.so` مع `libItsMyHubS.so` الأصلي (الذي لا يتم تحميله داخل المعاينة).
- زر جديد في شاشة التشخيص يفتح معاينة ImGui؛ بها اختبارات التبويبات، اللمس، الألوان، الشفافية، والسلايدر.
- بيئة GitHub Actions تتحقق من وجود `NativePreviewActivity` داخل DEX ومكتبة `libnova_preview.so` ضمن APK.

## شلون تجربها؟
1. حمّل ZIP من الـArtifact الناجح باسم `NOVA-real-imgui-preview-apk` في GitHub Actions.
2. فك الضغط عن `app-debug.apk` وثبته.
3. افتح **NOVA Native Diagnostics**.
4. اضغط «فتح منيو ImGui الأصلي — تجربة رسم ولمس».
5. افتح تبويبات Home / Style / Test. إذا كانت تتحرك وتستجيب للمس فمحرك العرض JNI/OpenGL اشتغل على جهازك.

## الحدود مهمة
- هذا **ليس منيو شغال داخل PUBG**. المعاينة تستخدم نفس مكتبة ImGui من المشروع، لكن تعرض نافذة منفصلة ضمن التطبيق نفسه، ولا تقرأ بيانات اللعبة.
- الهوكات الأصلية والمعالجات القديمة غير مُفعّلة في معاينة الرسم.
- لا تضمن هذه الخطوة التوافق مع آخر إصدار للعبة ولا تفعيل ESP/Aim/Skin أو تجاوز مكافحة الغش.
- فحص البناء يثبت إنشاء APK وتغليف المكتبة، **ولا يثبت تشغيل OpenGL على كل جهاز**؛ يحتاج تجربة فعلية من المستخدم.
- فرع التشخيص وفروع التعديلات محفوظة منفصلة، ولا يُدمج هذا في `main` تلقائياً.

## للمطور
`app/src/main/java/com/RIYAZSRC/NativePreviewActivity.java`
`app/src/main/jni/ImGui/NovaNativePreview.cpp`
`app/src/main/jni/Android.mk`

PR: https://github.com/galebandabood-sys/pubgmsrc/pull/5
