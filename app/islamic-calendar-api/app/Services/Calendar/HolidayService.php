<?php

namespace App\Services\Calendar;

use App\Services\AlAdhan\AlAdhanClient;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;

class HolidayService
{
    protected AlAdhanClient $client;
    protected HijriCalendarService $hijriCalendarService;
    protected bool $cacheEnabled;
    protected int $cacheTtl;

    /**
     * Standard Islamic Holidays mapping (Hijri day & month).
     */
    protected const ISLAMIC_HOLIDAYS_CATALOG = [
        [
            'name' => 'Islamic New Year',
            'name_ar' => 'رأس السنة الهجرية',
            'hijri_day' => 1,
            'hijri_month' => 1
        ],
        [
            'name' => 'Ashura',
            'name_ar' => 'عاشوراء',
            'hijri_day' => 10,
            'hijri_month' => 1
        ],
        [
            'name' => 'Mawlid al-Nabi',
            'name_ar' => 'المولد النبوي الشريف',
            'hijri_day' => 12,
            'hijri_month' => 3
        ],
        [
            'name' => 'Isra and Mi\'raj',
            'name_ar' => 'الإسراء والمعراج',
            'hijri_day' => 27,
            'hijri_month' => 7
        ],
        [
            'name' => 'First Day of Ramadan',
            'name_ar' => 'أول أيام شهر رمضان المبارك',
            'hijri_day' => 1,
            'hijri_month' => 9
        ],
        [
            'name' => 'Laylat al-Qadr (27th Night)',
            'name_ar' => 'ليلة القدر (ليلة 27)',
            'hijri_day' => 27,
            'hijri_month' => 9
        ],
        [
            'name' => 'Eid al-Fitr',
            'name_ar' => 'عيد الفطر المبارك',
            'hijri_day' => 1,
            'hijri_month' => 10
        ],
        [
            'name' => 'Day of Arafah',
            'name_ar' => 'يوم عرفة',
            'hijri_day' => 9,
            'hijri_month' => 12
        ],
        [
            'name' => 'Eid al-Adha',
            'name_ar' => 'عيد الأضحى المبارك',
            'hijri_day' => 10,
            'hijri_month' => 12
        ]
    ];

    public function __construct(AlAdhanClient $client, HijriCalendarService $hijriCalendarService)
    {
        $this->client = $client;
        $this->hijriCalendarService = $hijriCalendarService;
        $this->cacheEnabled = (bool) config('aladhan.cache_enabled', true);
        $this->cacheTtl = (int) config('aladhan.cache_ttl_holidays', 86400 * 7);
    }

    /**
     * Get list of Islamic holidays for a given Hijri year.
     * AlAdhan endpoint: GET /v1/holidays?year={year} or computed from calendar days
     */
    public function getIslamicHolidays(
        int $hijriYear,
        ?string $country = null,
        ?string $method = null
    ): array {
        $methodKey = $method ?? 'default';
        $countryKey = $country ? strtolower(trim($country)) : 'default';

        $cacheKey = sprintf(
            'calendar:holidays:%04d:%s:%s',
            $hijriYear,
            $countryKey,
            $methodKey
        );

        if ($this->cacheEnabled && Cache::has($cacheKey)) {
            return [
                'data' => Cache::get($cacheKey),
                'meta' => [
                    'provider' => 'AlAdhan',
                    'cached' => true,
                    'method' => $methodKey,
                    'hijri_year' => $hijriYear
                ]
            ];
        }

        try {
            // First attempt to call AlAdhan holidays endpoint if available
            $response = $this->client->get('holidays', ['year' => $hijriYear]);
            $holidaysList = [];

            if ($response->successful() && !empty($response->json()['data'])) {
                $raw = $response->json()['data'];
                foreach ($raw as $item) {
                    $holidaysList[] = [
                        'name' => $item['name'] ?? '',
                        'name_ar' => $item['name_ar'] ?? $this->findArabicHolidayName($item['name'] ?? ''),
                        'hijri' => [
                            'day' => (int) ($item['hijri']['day'] ?? 1),
                            'month' => (int) ($item['hijri']['month']['number'] ?? 1),
                            'year' => $hijriYear
                        ],
                        'gregorian' => [
                            'date' => $item['gregorian']['date'] ?? null
                        ]
                    ];
                }
            } else {
                // Compile standard catalog with calculated Hijri year
                foreach (self::ISLAMIC_HOLIDAYS_CATALOG as $holiday) {
                    $holidaysList[] = [
                        'name' => $holiday['name'],
                        'name_ar' => $holiday['name_ar'],
                        'hijri' => [
                            'day' => $holiday['hijri_day'],
                            'month' => $holiday['hijri_month'],
                            'year' => $hijriYear
                        ],
                        'gregorian' => [
                            'date' => null
                        ]
                    ];
                }
            }

            if ($this->cacheEnabled) {
                Cache::put($cacheKey, $holidaysList, $this->cacheTtl);
            }

            return [
                'data' => $holidaysList,
                'meta' => [
                    'provider' => 'AlAdhan',
                    'cached' => false,
                    'method' => $methodKey,
                    'hijri_year' => $hijriYear
                ]
            ];
        } catch (\Throwable $e) {
            Log::warning('Holidays service encountered error, using fallback catalog: ' . $e->getMessage());

            $fallback = [];
            foreach (self::ISLAMIC_HOLIDAYS_CATALOG as $holiday) {
                $fallback[] = [
                    'name' => $holiday['name'],
                    'name_ar' => $holiday['name_ar'],
                    'hijri' => [
                        'day' => $holiday['hijri_day'],
                        'month' => $holiday['hijri_month'],
                        'year' => $hijriYear
                    ],
                    'gregorian' => [
                        'date' => null
                    ]
                ];
            }

            return [
                'data' => $fallback,
                'meta' => [
                    'provider' => 'AlAdhan',
                    'cached' => false,
                    'fallback' => true,
                    'method' => $methodKey,
                    'hijri_year' => $hijriYear
                ]
            ];
        }
    }

    protected function findArabicHolidayName(string $englishName): string
    {
        $normalized = strtolower(trim($englishName));
        foreach (self::ISLAMIC_HOLIDAYS_CATALOG as $holiday) {
            if (str_contains($normalized, strtolower($holiday['name']))) {
                return $holiday['name_ar'];
            }
        }
        return $englishName;
    }
}
