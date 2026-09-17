<?php

namespace App\Services\Calendar;

use App\Services\AlAdhan\AlAdhanClient;
use Carbon\Carbon;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;

class GregorianCalendarService
{
    protected AlAdhanClient $client;
    protected bool $cacheEnabled;
    protected int $cacheTtl;

    public function __construct(AlAdhanClient $client)
    {
        $this->client = $client;
        $this->cacheEnabled = (bool) config('aladhan.cache_enabled', true);
        $this->cacheTtl = (int) config('aladhan.cache_ttl_month', 86400 * 30);
    }

    /**
     * Get full Gregorian month calendar with corresponding Hijri dates for each day.
     * AlAdhan endpoint: GET /v1/gToHCalendar/{month}/{year}
     */
    public function getMonth(
        int $month,
        int $year,
        ?string $country = null,
        ?string $method = null,
        int $adjustment = 0
    ): array {
        $methodKey = $method ?? 'default';
        $countryKey = $country ? strtolower(trim($country)) : 'default';

        $cacheKey = sprintf(
            'calendar:gregorian-month:%04d:%02d:%s:%s:%d',
            $year,
            $month,
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

            $response = $this->client->get("gToHCalendar/{$month}/{$year}", $params);

            if (!$response->successful()) {
                Log::error("AlAdhan gToHCalendar failed: {$response->status()}", ['body' => $response->body()]);
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $json = $response->json();
            $rawDays = $json['data'] ?? [];

            $days = [];
            foreach ($rawDays as $dayItem) {
                $greg = $dayItem['gregorian'] ?? [];
                $hijri = $dayItem['hijri'] ?? [];

                $gYear = (int) ($greg['year'] ?? $year);
                $gMonth = (int) ($greg['month']['number'] ?? $month);
                $gDay = (int) ($greg['day'] ?? 1);
                $stdDate = sprintf('%04d-%02d-%02d', $gYear, $gMonth, $gDay);

                $hDay = (int) ($hijri['day'] ?? 1);
                $hMonth = (int) ($hijri['month']['number'] ?? 1);
                $hYear = (int) ($hijri['year'] ?? 1448);
                $hDate = sprintf('%02d-%02d-%04d', $hDay, $hMonth, $hYear);

                $weekdayEn = (string) ($greg['weekday']['en'] ?? '');
                $weekdayAr = $this->getArabicWeekdayFromName($weekdayEn);

                $days[] = [
                    'gregorian' => [
                        'day' => $gDay,
                        'month' => $gMonth,
                        'year' => $gYear,
                        'date' => $stdDate,
                        'weekday' => [
                            'english' => $weekdayEn,
                            'arabic' => $weekdayAr
                        ]
                    ],
                    'hijri' => [
                        'day' => $hDay,
                        'month' => $hMonth,
                        'month_name' => (string) ($hijri['month']['en'] ?? ''),
                        'month_name_ar' => (string) ($hijri['month']['ar'] ?? ''),
                        'year' => $hYear,
                        'date' => $hDate,
                        'holidays' => $hijri['holidays'] ?? []
                    ]
                ];
            }

            $result = [
                'calendar_type' => 'gregorian',
                'month' => $month,
                'year' => $year,
                'days' => $days
            ];

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

    protected function getArabicWeekdayFromName(string $en): string
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
