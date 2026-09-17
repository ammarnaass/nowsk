# Islamic Calendar API (خدمة التقويم الهجري والميلادي المستقلة)

خدمة REST API موحدة ومستقلة مبنية بإطار العمل **Laravel 13 / PHP 8.3+** وتعتمد على **Redis** لتخزين البيانات مؤقتاً ومحاذاة بيانات **AlAdhan Calendar API**. صُممت خصيصاً لتزويد تطبيق **نسكّر** بجميع وظائف التقويم في مكان موحد دون الحاجة لإجراء تحويلات غير دقيقة داخل التطبيق المحلي.

---

## 1. المميزات والمواصفات المعمارية

* **تحويل مزدوج في الاتجاهين:**
  * تحويل ميلادي إلى هجري (`gregorian-to-hijri`).
  * تحويل هجري إلى ميلادي (`hijri-to-gregorian`).
* **التقويم الشهري الكامل:**
  * تقويم ميلادي مع المقابل الهجري لكل يوم (`/calendar/gregorian`).
  * تقويم هجري مع المقابل الميلادي لكل يوم (`/calendar/hijri`).
* **تاريخ اليوم التلقائي (`/calendar/today`):**
  * دعم كامل للمنطقة الزمنية المخصصة (Timezone) مثل `Africa/Algiers`.
* **المناسبات والأعياد الإسلامية (`/calendar/holidays`):**
  * دعم عرض المناسبات (المولد النبوي، رمضان، ليلة القدر، عيد الفطر، يوم عرفة، عيد الأضحى، عاشوراء، رأس السنة الهجرية) باللغتين العربية والإنجليزية.
* **دعم الدولة وطرق التقويم والتعديل (`adjustment`):**
  * إمكانية ضبط التعديل بـ `+1` أو `-1` مع الفصل الصارم بين التاريخ الأصلي (`source_hijri_date`) وتاريخ العرض (`display_hijri_date`).
* **استراتيجية التخزين المؤقت المتقدمة (Redis Caching & Stale-Fallback):**
  * كاش للتحويلات والشهور لمدة 30 يوماً.
  * كاش للأعياد لمدة 7 أيام.
  * في حال انقطاع خدمة AlAdhan الخارجية يتم توفير البيانات المخزنة من الكاش تلقائياً لحماية التطبيق من التعطل.
* **الحد من معدل الطلبات (Rate Limiting):**
  * 60 طلب في الدقيقة لكل عنوان IP لضمان استقرار الخدمة وتجنب الحظر.

---

## 2. نقاط النهاية (Endpoints)

| الوظيفة | الطريقة | المسار (Endpoint) | المعلمات (Parameters) |
|---|---|---|---|
| **تاريخ اليوم** | `GET` | `/api/v1/calendar/today` | `timezone`, `country`, `method`, `adjustment` |
| **تحويل ميلادي ← هجري** | `GET` | `/api/v1/calendar/gregorian-to-hijri` | `date` (YYYY-MM-DD), `country`, `method`, `adjustment` |
| **تحويل هجري ← ميلادي** | `GET` | `/api/v1/calendar/hijri-to-gregorian` | `date` (DD-MM-YYYY), `country`, `method`, `adjustment` |
| **التقويم الشهري الميلادي** | `GET` | `/api/v1/calendar/gregorian` | `month` (1-12), `year`, `country`, `method`, `adjustment` |
| **التقويم الشهري الهجري** | `GET` | `/api/v1/calendar/hijri` | `month` (1-12), `year`, `country`, `method`, `adjustment` |
| **المناسبات الإسلامية** | `GET` | `/api/v1/calendar/holidays` | `year` (هجري), `country`, `method` |
| **فحص جاهزية الخدمة** | `GET` | `/api/health` | لا يوجد |

---

## 3. أمثلة الاستجابة (JSON Structure)

### أ. تحويل ميلادي إلى هجري (`GET /api/v1/calendar/gregorian-to-hijri?date=2026-09-17`)
```json
{
  "success": true,
  "data": {
    "gregorian": {
      "date": "2026-09-17",
      "day": 17,
      "month": 9,
      "year": 2026,
      "weekday": {
        "number": 4,
        "english": "Thursday",
        "arabic": "الخميس"
      }
    },
    "hijri": {
      "date": "25-03-1448",
      "source_hijri_date": "25-03-1448",
      "display_hijri_date": "25-03-1448",
      "day": 25,
      "month": 3,
      "month_name": "Rabi al-awwal",
      "month_name_ar": "ربيع الأول",
      "year": 1448
    },
    "adjustment": 0
  },
  "meta": {
    "provider": "AlAdhan",
    "cached": false,
    "method": "default",
    "adjustment": 0
  }
}
```

---

## 4. التشغيل عبر Docker

```bash
cd islamic-calendar-api
docker compose up -d --build
```

ستعمل الحاويات التالية:
* **`islamic_calendar_app`**: PHP 8.3-FPM مع الامتدادات المطلوبة.
* **`islamic_calendar_nginx`**: خادم Nginx على المنفذ `8001`.
* **`islamic_calendar_redis`**: خادم Redis للكاش.
* **`islamic_calendar_mysql`**: قاعدة بيانات MySQL 8.0.

---

## 5. التوثيق والاعتماد (Attribution)

* **OpenAPI Spec:** متاح في المسار `docs/openapi.yaml`.
* **Attribution:** Islamic Calendar data powered by [AlAdhan API](https://aladhan.com/islamic-calendar-api).
