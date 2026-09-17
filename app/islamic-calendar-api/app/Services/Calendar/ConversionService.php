<?php

namespace App\Services\Calendar;

use App\Services\AlAdhan\AlAdhanClient;
use Carbon\Carbon;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;

class ConversionService
{
    protected AlAdhanClient $client;
    protected bool $cacheEnabled;
    protected int $cacheTtl;

    public function __construct(AlAdhanClient $client)
    {
        $this->client = $client;
        $this->cacheEnabled = (bool) config('aladhan.cache_enabled', true);
        $this->cacheTtl = (int) config('aladhan.cache_ttl_conversion', 86400 * 30);
    }

    /**
     * Convert Gregorian date (YYYY-MM-DD) to Hijri date.
     * AlAdhan endpoint: GET /v1/gToH/{date} (date format: DD-MM-YYYY)
     */
    public function convertGregorianToHijri(
        string $gregorianDateStr,
        ?string $country = null,
        ?string $method = null,
        int $adjustment = 0
    ): array {
        // Parse input YYYY-MM-DD
        $carbon = Carbon::createFromFormat('Y-m-d', $gregorianDateStr);
        $aladhanDateParam = $carbon->format('d-m-Y');

        $methodKey = $method ?? 'default';
        $countryKey = $country ? strtolower(trim($country)) : 'default';

        $cacheKey = sprintf(
            'calendar:gregorian:%s:%s:%s:%d',
            $gregorianDateStr,
            $countryKey,
            $methodKey,
            $adjustment
        );

        if ($this->cacheEnabled && Cache::has($cacheKey)) {
            return [
                'data' => Cache::get($cacheKey),
                'meta' => [
                    'provider' => 'AlAdhan',
                    'cached' => true,
                    'method' => $methodKey,
                    'adjustment' => $adjustment
                ]
            ];
        }

        try {
            $params = [];
            if ($adjustment !== 0) {
                $params['adjustment'] = $adjustment;
            }
            if ($method && $method !== 'default') {
                $params['calendarMethod'] = $method;
            }

            $response = $this->client->get("gToH/{$aladhanDateParam}", $params);

            if (!$response->successful()) {
                Log::error("AlAdhan gToH failed: {$response->status()}", ['body' => $response->body()]);
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $json = $response->json();
            $data = $json['data'] ?? [];

            $result = $this->normalizeConversionData($data, $carbon, $adjustment);

            if ($this->cacheEnabled) {
                Cache::put($cacheKey, $result, $this->cacheTtl);
                Cache::put("stale:{$cacheKey}", $result, $this->cacheTtl * 2);
            }

            return [
                'data' => $result,
                'meta' => [
                    'provider' => 'AlAdhan',
                    'cached' => false,
                    'method' => $methodKey,
                    'adjustment' => $adjustment
                ]
            ];
        } catch (\Throwable $e) {
            if ($this->cacheEnabled && Cache::has("stale:{$cacheKey}")) {
                return [
                    'data' => Cache::get("stale:{$cacheKey}"),
                    'meta' => [
                        'provider' => 'AlAdhan',
                        'cached' => true,
                        'stale' => true,
                        'method' => $methodKey,
                        'adjustment' => $adjustment
                    ]
                ];
            }
            throw $e;
        }
    }

    /**
     * Convert Hijri date (DD-MM-YYYY) to Gregorian date.
     * AlAdhan endpoint: GET /v1/hToG/{date}
     */
    public function convertHijriToGregorian(
        string $hijriDateStr,
        ?string $country = null,
        ?string $method = null,
        int $adjustment = 0
    ): array {
        $methodKey = $method ?? 'default';
        $countryKey = $country ? strtolower(trim($country)) : 'default';

        $cacheKey = sprintf(
            'calendar:hijri:%s:%s:%s:%d',
            $hijriDateStr,
            $countryKey,
            $methodKey,
            $adjustment
        );

        if ($this->cacheEnabled && Cache::has($cacheKey)) {
            return [
                'data' => Cache::get($cacheKey),
                'meta' => [
                    'provider' => 'AlAdhan',
                    'cached' => true,
                    'method' => $methodKey,
                    'adjustment' => $adjustment
                ]
            ];
        }

        try {
            $params = [];
            if ($adjustment !== 0) {
                $params['adjustment'] = $adjustment;
            }
            if ($method && $method !== 'default') {
                $params['calendarMethod'] = $method;
            }

            $response = $this->client->get("hToG/{$hijriDateStr}", $params);

            if (!$response->successful()) {
                Log::error("AlAdhan hToG failed: {$response->status()}", ['body' => $response->body()]);
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $json = $response->json();
            $data = $json['data'] ?? [];

            $result = $this->normalizeReverseConversionData($data, $hijriDateStr, $adjustment);

            if ($this->cacheEnabled) {
                Cache::put($cacheKey, $result, $this->cacheTtl);
                Cache::put("stale:{$cacheKey}", $result, $this->cacheTtl * 2);
            }

            return [
                'data' => $result,
                'meta' => [
                    'provider' => 'AlAdhan',
                    'cached' => false,
                    'method' => $methodKey,
                    'adjustment' => $adjustment
                ]
            ];
        } catch (\Throwable $e) {
            if ($this->cacheEnabled && Cache::has("stale:{$cacheKey}")) {
                return [
                    'data' => Cache::get("stale:{$cacheKey}"),
                    'meta' => [
                        'provider' => 'AlAdhan',
                        'cached' => true,
                        'stale' => true,
                        'method' => $methodKey,
                        'adjustment' => $adjustment
                    ]
                ];
            }
            throw $e;
        }
    }

    protected function normalizeConversionData(array $data, Carbon $gregorianCarbon, int $adjustment): array
    {
        $hijriRaw = $data['hijri'] ?? [];
        $gregRaw = $data['gregorian'] ?? [];

        $hDay = (int) ($hijriRaw['day'] ?? 1);
        $hMonthNum = (int) ($hijriRaw['month']['number'] ?? 1);
        $hMonthEn = (string) ($hijriRaw['month']['en'] ?? '');
        $hMonthAr = (string) ($hijriRaw['month']['ar'] ?? '');
        $hYear = (int) ($hijriRaw['year'] ?? 1448);
        $hFormattedDate = sprintf('%02d-%02d-%04d', $hDay, $hMonthNum, $hYear);

        $sourceHijriDate = $hFormattedDate;
        $displayHijriDate = $hFormattedDate;

        if ($adjustment !== 0) {
            $adjustedDay = max(1, min(30, $hDay + $adjustment));
            $displayHijriDate = sprintf('%02d-%02d-%04d', $adjustedDay, $hMonthNum, $hYear);
        }

        $weekdayEn = (string) ($gregRaw['weekday']['en'] ?? $gregorianCarbon->format('l'));
        $weekdayAr = $this->getArabicWeekday($gregorianCarbon->dayOfWeek);

        return [
            'gregorian' => [
                'date' => $gregorianCarbon->format('Y-m-d'),
                'day' => (int) $gregorianCarbon->format('d'),
                'month' => (int) $gregorianCarbon->format('m'),
                'year' => (int) $gregorianCarbon->format('Y'),
                'weekday' => [
                    'number' => $gregorianCarbon->dayOfWeekIso,
                    'english' => $weekdayEn,
                    'arabic' => $weekdayAr
                ]
            ],
            'hijri' => [
                'date' => $hFormattedDate,
                'source_hijri_date' => $sourceHijriDate,
                'display_hijri_date' => $displayHijriDate,
                'day' => $hDay,
                'month' => $hMonthNum,
                'month_name' => $hMonthEn,
                'month_name_ar' => $hMonthAr,
                'year' => $hYear,
                'holidays' => $hijriRaw['holidays'] ?? []
            ],
            'adjustment' => $adjustment
        ];
    }

    protected function normalizeReverseConversionData(array $data, string $origHijriStr, int $adjustment): array
    {
        $hijriRaw = $data['hijri'] ?? [];
        $gregRaw = $data['gregorian'] ?? [];

        $gDateStr = (string) ($gregRaw['date'] ?? ''); // e.g. "17-09-2026"
        $gYear = (int) ($gregRaw['year'] ?? 2026);
        $gMonth = (int) ($gregRaw['month']['number'] ?? 9);
        $gDay = (int) ($gregRaw['day'] ?? 17);

        $stdGDate = sprintf('%04d-%02d-%02d', $gYear, $gMonth, $gDay);

        $hDay = (int) ($hijriRaw['day'] ?? 1);
        $hMonthNum = (int) ($hijriRaw['month']['number'] ?? 1);
        $hMonthEn = (string) ($hijriRaw['month']['en'] ?? '');
        $hMonthAr = (string) ($hijriRaw['month']['ar'] ?? '');
        $hYear = (int) ($hijriRaw['year'] ?? 1448);
        $hFormattedDate = sprintf('%02d-%02d-%04d', $hDay, $hMonthNum, $hYear);

        return [
            'hijri' => [
                'date' => $hFormattedDate,
                'day' => $hDay,
                'month' => $hMonthNum,
                'month_name' => $hMonthEn,
                'month_name_ar' => $hMonthAr,
                'year' => $hYear,
                'holidays' => $hijriRaw['holidays'] ?? []
            ],
            'gregorian' => [
                'date' => $stdGDate,
                'day' => $gDay,
                'month' => $gMonth,
                'year' => $gYear,
                'weekday' => [
                    'english' => (string) ($gregRaw['weekday']['en'] ?? ''),
                    'arabic' => $this->getArabicWeekdayFromName((string) ($gregRaw['weekday']['en'] ?? ''))
                ]
            ],
            'adjustment' => $adjustment
        ];
    }

    public function getArabicWeekday(int $carbonDayOfWeek): string
    {
        return match ($carbonDayOfWeek) {
            Carbon::SUNDAY => 'الأحد',
            Carbon::MONDAY => 'الإثنين',
            Carbon::TUESDAY => 'الثلاثاء',
            Carbon::WEDNESDAY => 'الأربعاء',
            Carbon::THURSDAY => 'الخميس',
            Carbon::FRIDAY => 'الجمعة',
            Carbon::SATURDAY => 'السبت',
            default => ''
        };
    }

    public function getArabicWeekdayFromName(string $en): string
    {
        return match (strtolower(trim($en))) {
            'sunday' => 'الأحد',
            'monday' => 'الإثنين',
            'tuesday' => 'الثلاثاء',
            'wednesday' => 'الأربعاء',
            'thursday' => 'الخميس',
            'friday' => 'الجمعة',
            'saturday' => 'السبت',
            default => ''
        };
    }
}
