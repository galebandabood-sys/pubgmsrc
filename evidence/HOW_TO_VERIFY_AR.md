# تدقيق نسخة PUBG Mobile العالمية 4.7.0 — بدون روت

هذا الفحص يقارن `libUE4.so` داخل أرشيف اللعبة بالبصمة التي نقلها المستخدم من MT Manager:
`evidence/pubg-global-4.7.0-21740-libue4-observed.json`.

**لا يستخرج أوفستات، ولا يحقن أكواد، ولا يشغّل درايفر، ولا يصلح ESP أو Aimbot.**
حتى نتيجة `MATCHED_BINARY_ONLY` تؤكد هوية الملف فقط، وليس توافق SDK أو عمل أي خاصية.

## فحص ملفات اللعبة المستخرجة

المطلوب: Python 3 ومساحة مؤقتة كافية لحوالي 121 MB لفك Split APK.

```sh
python3 tools/verify_pubg_split.py \
  --input "/storage/emulated/0/MT2/apks/PUBG MOBILE_4.7.0.apks" \
  --reference evidence/pubg-global-4.7.0-21740-libue4-observed.json
```

يدعم أيضاً `split_config.arm64_v8a.apk` أو `libUE4.so` المنسوخين سابقاً من اللعبة.

- `MATCHED_BINARY_ONLY`: SHA-256 والحجم ومعمارية ELF متطابقة مع الملف الموثّق.
- `FAIL`: اختلاف البصمة أو الحجم أو المعمارية.
- `UNVERIFIED`: ملف أو مرجع غير متوفر، أو صيغة أرشيف غير قابلة للقراءة.

المرجع مصدره صورة حاسبة MT Manager للمستخدم، ولم يتم استلام ملف المحرك نفسه. لذلك تعتبر هذه خطوة تحقق إضافية لا دليلاً على أن SDK القديم مناسب لـ4.7.
