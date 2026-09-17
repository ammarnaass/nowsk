<?php

namespace App\Services\AlAdhan;

use App\Services\Calendar\ConversionService;
use App\Services\Calendar\GregorianCalendarService;
use App\Services\Calendar\HijriCalendarService;
use App\Services\Calendar\HolidayService;
use Carbon\Carbon;

class CalendarService
{
    protected ConversionService $conversionService;
    protected GregorianCalendarService $gregorianCalendarService;
    protected HijriCalendarService $hijriCalendarService;
    protected HolidayService $holidayService;

    public function __construct(
        ConversionService $conversionService,
        GregorianCalendarService $gregorianCalendarService,
        HijriCalendarService $hijriCalendarService,
        HolidayService $holidayService
    ) {
        $this->conversionService = $conversionService;
        $this->gregorianCalendarService = $gregorianCalendarService;
        $this->hijriCalendarService = $hijriCalendarService;
        $this->holidayService = $holidayService;
    }

    /**
     * Get today's Gregorian and Hijri dates for the specified or default timezone.
     */
    public function getToday(
        ?string $timezone = null,
        ?string $country = null,
        ?string $method = null,
        int $adjustment = 0
    ): array {
        $tz = $timezone ?: config('aladhan.default_timezone', 'Africa/Algiers');
        
        try {
            $now = Carbon::now($tz);
        } catch (\Throwable $e) {
            $tz = config('aladhan.default_timezone', 'Africa/Algiers');
            $now = Carbon::now($tz);
        }

        $gregorianDateStr = $now->format('Y-m-d');

        $result = $this->conversionService->convertGregorianToHijri(
            $gregorianDateStr,
            $country,
            $method,
            $adjustment
        );

        $data = $result['data'];
        $meta = $result['meta'];
        $meta['timezone'] = $tz;

        return [
            'data' => [
                'today' => [
                    'gregorian' => $data['gregorian'],
                    'hijri' => $data['hijri']
                ]
            ],
            'meta' => $meta
        ];
    }

    /**
     * Convert Gregorian date to Hijri.
     */
    public function convertGregorianToHijri(
        string $date,
        ?string $country = null,
        ?string $method = null,
        int $adjustment = 0
    ): array {
        return $this->conversionService->convertGregorianToHijri(
            $date,
            $country,
            $method,
            $adjustment
        );
    }

    /**
     * Convert Hijri date to Gregorian.
     */
    public function convertHijriToGregorian(
        string $date,
        ?string $country = null,
        ?string $method = null,
        int $adjustment = 0
    ): array {
        return $this->conversionService->convertHijriToGregorian(
            $date,
            $country,
            $method,
            $adjustment
        );
    }

    /**
     * Full Gregorian Month calendar.
     */
    public function getGregorianCalendar(
        int $month,
        int $year,
        ?string $country = null,
        ?string $method = null,
        int $adjustment = 0
    ): array {
        return $this->gregorianCalendarService->getMonth(
            $month,
            $year,
            $country,
            $method,
            $adjustment
        );
    }

    /**
     * Full Hijri Month calendar.
     */
    public function getHijriCalendar(
        int $month,
        int $year,
        ?string $country = null,
        ?string $method = null,
        int $adjustment = 0
    ): array {
        return $this->hijriCalendarService->getMonth(
            $month,
            $year,
            $country,
            $method,
            $adjustment
        );
    }

    /**
     * Islamic Holidays for given Hijri year.
     */
    public function getHolidays(
        int $year,
        ?string $country = null,
        ?string $method = null
    ): array {
        return $this->holidayService->getIslamicHolidays(
            $year,
            $country,
            $method
        );
    }
}
