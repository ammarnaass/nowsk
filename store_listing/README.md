# 📦 مجلد متطلبات نشر تطبيق نُسُك إسلامي (Store Listing & Assets)
## لنشر التطبيق على متجري Huawei AppGallery و Google Play Store

تم إعداد هذا المجلد خصيصاً ليتضمن كافة المستندات والتصاميم المطلوبة لرفع ونشر التطبيق والموافقة عليه بسلاسة وفقاً لأحدث سياسات المتاجر لعام 2026.

---

### 📂 هيكل المجلدات والملفات:

```text
store_listing/
├── README.md                               # دليل الاستخدام والفهرس الشامل
├── privacy_policy.md                       # سياسة الخصوصية العامة باللغتين العربية والإنجليزية
│
├── huawei_appgallery/                      # 🔴 قسم متجر هواوي AppGallery
│   ├── app_description_huawei.md          # نصوص الوصف الكامل والمختصر والكلمات المفتاحية
│   ├── app_metadata_huawei.json           # ملخص الحقول والبيانات التقنية لمتجر هواوي
│   └── privacy_policy.md                 # سياسة الخصوصية الخاصة بمراجعة هواوي
│
├── google_play/                            # 🟢 قسم متجر جوجل بلاي Google Play
│   ├── app_description_google_play.md     # الوصف المنضبط بحدود الحروف (العنوان ≤ 30، الوصف القصير ≤ 80)
│   ├── data_safety_declaration.md         # دليل تعبئة استبيان أمان البيانات (Data Safety Section)
│   └── privacy_policy.md                 # سياسة الخصوصية المعتمدة
│
└── assets/                                 # 🎨 الوسائط والتصاميم الرسمية
    ├── feature_graphic_1024x500.png       # البانر الترويجي المميز (Feature Graphic 1024×500)
    │
    ├── icon/                              # أيقونة وشعار التطبيق بالأحجام المعتمدة
    │   ├── icon_216x216.png              # الحجم المخصص لمتجر هواوي (216×216 px)
    │   ├── icon_512x512.png              # الحجم القياسي المعتمد لجوجل وهواوي (512×512 px)
    │   └── icon_1024x1024.png            # النسخة فائقة الدقة Master HD (1024×1024 px)
    │
    └── screenshots/                       # لقطات شاشة التطبيق الحقيقية (1080×2400 px)
        ├── 01_home_screen.png            # الشاشة الرئيسية ومواقيت الصلاة والعد التنازلي
        ├── 02_mushaf_reader.png          # المصحف الشريف بالرسم العثماني المطابق لطبعة المدينة
        ├── 03_quran_surahs.png           # فهرس سور القرآن الكريم والأجزاء مع البحث السريع
        ├── 04_dhikr_counter.png          # شاشة الأذكار التفاعلية والعداد اللمسي
        ├── 05_adhkar_categories.png      # فهرس أذكار حصن المسلم المتنوعة
        ├── 06_qibla_compass.png          # بوصلة القبلة الذكية والمسافة لمكة المكرمة
        ├── 07_tasbeeh_counter.png        # المسبحة الإلكترونية الذكية مع تصفير العداد
        ├── 08_islamic_calendar.png       # التقويم الهجري والمناسبات الإسلامية
        └── 09_onboarding_welcome.png     # واجهة الترحيب ومواقيت الصلاة الأولية
```

---

### 🚀 خطوات النشر السريع على Huawei AppGallery Connect:
1. ادخل إلى [Huawei AppGallery Connect](https://developer.huawei.com/consumer/en/service/josp/agc/index.html).
2. اختر **My Apps** > **New App** وأدخل اسم الحزمة `com.nusakkir.islamic` واسم التطبيق من ملف `app_description_huawei.md`.
3. ارفع ملف الأيقونة `assets/icon/icon_216x216.png` أو `icon_512x512.png`.
4. ارفع لقطات الشاشة من مجلد `assets/screenshots/` (اختر من 3 إلى 8 صور).
5. انسخ الوصف القصير والكامل من `app_description_huawei.md`.
6. أضف رابط سياسة الخصوصية من `privacy_policy.md`.
7. ارفع ملف الـ APK أو AAB الجاهز وأرسل التطبيق للمراجعة.

---

### 🚀 خطوات النشر على Google Play Console:
1. ادخل إلى Google Play Console وأنشئ تطبيقا جديدا.
2. ضع العنوان `نُسُك: مواقيت الصلاة والقرآن` (28 حرفاً).
3. ضع الوصف القصير (73 حرفاً) والكامل من `app_description_google_play.md`.
4. ارفع الأيقونة `assets/icon/icon_512x512.png` والبانر الترويجي `assets/feature_graphic_1024x500.png`.
5. ارفع لقطات الشاشة من `assets/screenshots/`.
6. املأ قسم أمان البيانات (Data Safety) وفق الإرشادات الموضحة في `data_safety_declaration.md`.
