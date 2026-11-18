# مراجعة توافق SDK بإثبات بصمة المكتبة

## المصادر المستفاد منها

- AndUE4Dumper: https://github.com/AndnixSH/AndUE4Dumper
- UE4Dumper: https://github.com/kp7742/UE4Dumper
- PTE offset skill: https://github.com/qiuye1314/pte-track-offset-skill

المشاريع منشورة، وبعضها يحتوي SDK أو وسيلة لتوليده ومقارنة الإصدارات، لكن أي بيانات تخص PUBG 4.6 العالمي أو PUBG 4.7.3 التجريبي لا تثبت توافق 4.7 العالمي. لم يتم نسخ كود حقن أو تجاوز حماية أو أرقام إزاحات مجهولة المصدر.

## التغيير التقني بالمشروع

أُضيف tools/sdk_compat_audit.py وهو أداة قراءة فقط. يأخذ:

- ملف app/src/main/jni/Offset.h الخاص بالمصدر الأصلي.
- ملف JSON مُوثق ببيانات SDK لإصدار اللعبة والحزمة المستهدفين.
- libUE4.so من نفس البناء للعبة المستهدفة، لكي يتحقق من بصمة SHA-256 ومعمارية ARM64.

يفحص ثمانية ثوابت أساسية، وتوافق اسم الحزمة والإصدار ونوع المعمارية، ويرفض البصمة أو الثوابت المختلفة.

## الاستخدام

الأمر الأساسي:
python3 tools/sdk_compat_audit.py --offsets app/src/main/jni/Offset.h --package com.tencent.ig --version 4.7.0

من دون بيانات مبنية على نفس ملف اللعبة سيعيد UNVERIFIED برمز خروج 2؛ هذه النتيجة صحيحة ولا تعني وجود توافق.

بعد توفر البيانات الموثقة أضف:
--profile path/to/profile.json --libue4 path/to/libUE4.so

صيغة profile.json المطلوبة:
{
  "schema": 1,
  "package": "com.tencent.ig",
  "game_version": "4.7.0",
  "abi": "arm64-v8a",
  "source_url": "SOURCE_URL",
  "libue4_sha256": "REAL_64_CHARACTER_SHA256",
  "offsets": {
    "GNames_Offset": "0x...",
    "GUObject_Offset": "0x..."
  }
}

هذا مثال ناقص عن قصد وليس SDK حقيقياً. يجب توثيق كل الثوابت المطلوبة وبصمة الملف الفعلية قبل القبول.

## النتائج

- تسعة اختبارات وحدة، من ضمنها اختلاف الإصدار، الحزمة، الإزاحة، بصمة المكتبة، ونوع ELF.
- GitHub Actions يرفع تقرير JSON بحالة UNVERIFIED لحين وجود دليل كامل.
- حتى نتيجة MATCHED_METADATA_ONLY لا تثبت أن ESP أو Aimbot أو No Recoil تعمل؛ إنها تثبت فقط مطابقة الميتاداتا.
- لم يتم ربط مكتبات اللعبة، ولم يتم بناء APK جديد، ولم يتم تغيير Main.cpp أو الترويسات القديمة تلقائياً.
