# Prayer Times API

خدمة REST API مستقلة موحدة لجلب وإدارة مواقيت الصلاة لتطبيق **«نسكّر — Nsakkr»** أو أي تطبيق إسلامي آخر، تعمل كطبقة حماية وتخزين مؤقت وسيطة مع **AlAdhan API**.

---

## 1. المميزات الرئيسية

* **Proxy & Cache Layer:** وسيط ذكي بين تطبيقات الهاتف وخدمة AlAdhan API.
* **Redis Caching:** تخزين مؤقت للاستجابات لتقليل استهلاك موارد الإنترنت وتحقيق زمن استجابة أقل من 100ms.
* **Stale-While-Revalidate Fallback:** في حال تعطل خدمة AlAdhan الخارجية مؤقتًا، يقوم النظام بإعادة البيانات المحفوظة سابقًا مع إشارة `stale: true`.
* **Data Normalization:** تطبيع وتنقية توقيتات الصلاة وإزالة زوائد المناطق الزمنية (مثل `05:01 (+01)` -> `05:01`).
* **Algeria Optimization:** دعم طريقة الحساب الجزائرية (وزارة الشؤون الدينية والأوقاف - المعرف 19) كخيار افتراضي مرن.
* **Comprehensive Endpoints:**
  * مواقيت الصلاة اليومية بالإحداثيات GPS.
  * مواقيت الصلاة بالمدينة والدولة.
  * مواقيت الصلاة بالعنوان النصي.
  * جدول مواقيت شهر كامل.
  * قائمة طرق الحساب المعتمدة.
* **Security & Rate Limiting:** تحديد معدل الطلبات لكل عنوان IP (60 طلب/دقيقة)، حماية CORS مخصصة، ومنع تسريب أخطاء AlAdhan الداخلية.
* **OpenAPI 3.0 Documentation:** مواصفات وتوثيق Swagger متاح في `openapi.yaml`.

---

## 2. متطلبات التشغيل

* PHP >= 8.2 (أو 8.3+)
* Composer 2.x
* Redis Server >= 6.0
* MySQL >= 8.0 (اختياري للـ MVP)
* Nginx / Apache أو Docker & Docker Compose

---

## 3. التثبيت والتشغيل المحلي

### أ. باستخدام Docker Compose (موصى به)

```bash
cd prayer-times-api
cp .env.example .env

# تشغيل الحاويات (PHP-FPM, Nginx, Redis, MySQL)
docker-compose up -d --build

# تثبيت الحزم وتوليد المفتاح
docker-compose exec app composer install
docker-compose exec app php artisan key:generate
```

سيعمل الخادم على: `http://localhost:8000`

### ب. التثبيت اليدوي على الخادم المحلي أو VPS

```bash
cd prayer-times-api
composer install

cp .env.example .env
php artisan key:generate

# تشغيل الخادم
php artisan serve --port=8000
```

---

## 4. متغيرات البيئة (.env)

| المتغير | القيمة الافتراضية | الوصف |
| :--- | :--- | :--- |
| `ALADHAN_BASE_URL` | `https://api.aladhan.com/v1` | عنوان خدمة AlAdhan |
| `ALADHAN_TIMEOUT` | `10` | المهلة القصوى بالثواني للطلب |
| `PRAYER_CACHE_ENABLED` | `true` | تفعيل الكاش في Redis |
| `PRAYER_CACHE_TTL` | `86400` | مدة بقاء الكاش (24 ساعة) |
| `PRAYER_RATE_LIMIT` | `60` | أقصى عدد طلبات لكل IP بالدقيقة |
| `REDIS_HOST` | `127.0.0.1` | عنوان خادم Redis |

---

## 5. نقاط النهاية (Endpoints)

### أ. فحص صحة الخدمة
```http
GET /api/health
```

### ب. مواقيت الصلاة اليومية بالإحداثيات
```http
GET /api/v1/prayer-times?latitude=35.7058&longitude=4.5419&date=2026-09-17&method=19&school=0
```

### ج. مواقيت الصلاة بالمدينة والدولة
```http
GET /api/v1/prayer-times/city?city=M'Sila&country=Algeria
```

### د. مواقيت الصلاة بالعنوان
```http
GET /api/v1/prayer-times/address?address=M'Sila, Algeria
```

### هـ. مواقيت شهر كامل
```http
GET /api/v1/prayer-times/monthly?latitude=35.7058&longitude=4.5419&month=9&year=2026
```

### و. طرق الحساب المتاحة
```http
GET /api/v1/methods
```

---

## 6. مثال على الاستجابة الناجحة

```json
{
  "success": true,
  "data": {
    "date": "2026-09-17",
    "location": {
      "latitude": 35.7058,
      "longitude": 4.5419
    },
    "timezone": "Africa/Algiers",
    "method": {
      "id": 19,
      "name": "Ministry of Religious Affairs and Wakfs, Algeria"
    },
    "timings": {
      "fajr": "05:01",
      "sunrise": "06:29",
      "dhuhr": "12:34",
      "asr": "16:08",
      "sunset": "18:42",
      "maghrib": "18:42",
      "isha": "20:03",
      "imsak": "04:51",
      "midnight": "00:35"
    },
    "hijri": {
      "day": "25",
      "month": 3,
      "monthName": "Rabi al-awwal",
      "year": "1448"
    }
  },
  "meta": {
    "provider": "AlAdhan",
    "cached": true,
    "stale": false
  }
}
```

---

## 7. مثال على استجابة الخطأ الموحدة

```json
{
  "success": false,
  "error": {
    "code": "INVALID_LATITUDE",
    "message": "Latitude must be between -90 and 90."
  }
}
```

---

## 8. تشغيل الاختبارات (Tests)

```bash
# تشغيل جميع اختبارات Feature و Unit
./vendor/bin/phpunit
```

---

## 9. النشر في بيئة الإنتاج (Production Deployment)

```bash
composer install --no-dev --optimize-autoloader
php artisan config:cache
php artisan route:cache
php artisan view:cache
```
