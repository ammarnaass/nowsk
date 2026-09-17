<?php

namespace App\Services\AlAdhan;

use App\Services\AlAdhan\AlAdhanClient;
use Carbon\Carbon;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;

class PrayerTimesService
{
    protected AladhanClient $client;
    protected bool $cacheEnabled;
    protected int $cacheTtl;

    public function __construct(AlAdhanClient $client)
    {
        $this->client = $client;
        $this->cacheEnabled = config('aladhan.cache_enabled', true);
        $this->cacheTtl = config('aladhan.cache_ttl', 86400);
    }

    /**
     * Get daily prayer times by GPS coordinates.
     */
    public function getTimingsByCoordinates(
        float $latitude,
        float $longitude,
        ?string $date = null,
        ?int $method = null,
        ?int $school = null,
        ?string $tune = null
    ): array {
        $parsedDate = $this->resolveDate($date);
        $methodId = $method ?? (int) config('aladhan.default_method', 19);
        $schoolId = $school ?? (int) config('aladhan.default_school', 0);

        $cacheKey = sprintf(
            'prayer:%s:%s:%s:%d:%d:%s',
            $parsedDate->format('Y-m-d'),
            number_format($latitude, 4, '.', ''),
            number_format($longitude, 4, '.', ''),
            $methodId,
            $schoolId,
            $tune ?? 'none'
        );

        return $this->rememberWithFallback($cacheKey, function () use ($parsedDate, $latitude, $longitude, $methodId, $schoolId, $tune) {
            $dateParam = $parsedDate->format('d-m-Y');
            $params = [
                'latitude' => $latitude,
                'longitude' => $longitude,
                'method' => $methodId,
                'school' => $schoolId,
            ];
            if (!empty($tune)) {
                $params['tune'] = $tune;
            }

            $response = $this->client->get("timings/{$dateParam}", $params);

            if (!$response->successful()) {
                Log::error("AlAdhan API error in timings: Status {$response->status()}", ['body' => $response->body()]);
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $json = $response->json();
            if (!isset($json['data'])) {
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            return $this->normalizeDailyData($json['data'], $latitude, $longitude, $parsedDate->format('Y-m-d'));
        });
    }

    /**
     * Get daily prayer times by City and Country.
     */
    public function getTimingsByCity(
        string $city,
        string $country,
        ?string $date = null,
        ?int $method = null,
        ?int $school = null,
        ?string $tune = null
    ): array {
        $parsedDate = $this->resolveDate($date);
        $methodId = $method ?? (int) config('aladhan.default_method', 19);
        $schoolId = $school ?? (int) config('aladhan.default_school', 0);

        $cleanCity = strtolower(trim($city));
        $cleanCountry = strtolower(trim($country));

        $cacheKey = sprintf(
            'prayer:city:%s:%s:%s:%d:%d:%s',
            $cleanCity,
            $cleanCountry,
            $parsedDate->format('Y-m-d'),
            $methodId,
            $schoolId,
            $tune ?? 'none'
        );

        return $this->rememberWithFallback($cacheKey, function () use ($parsedDate, $city, $country, $methodId, $schoolId, $tune) {
            $dateParam = $parsedDate->format('d-m-Y');
            $params = [
                'city' => $city,
                'country' => $country,
                'method' => $methodId,
                'school' => $schoolId,
            ];
            if (!empty($tune)) {
                $params['tune'] = $tune;
            }

            $response = $this->client->get("timingsByCity/{$dateParam}", $params);

            if (!$response->successful()) {
                Log::error("AlAdhan API error in timingsByCity: {$response->status()}");
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $json = $response->json();
            if (!isset($json['data'])) {
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $rawMeta = $json['data']['meta'] ?? [];
            $lat = (float) ($rawMeta['latitude'] ?? 0.0);
            $lng = (float) ($rawMeta['longitude'] ?? 0.0);

            return $this->normalizeDailyData($json['data'], $lat, $lng, $parsedDate->format('Y-m-d'));
        });
    }

    /**
     * Get daily prayer times by Address string.
     */
    public function getTimingsByAddress(
        string $address,
        ?string $date = null,
        ?int $method = null,
        ?int $school = null,
        ?string $tune = null
    ): array {
        $parsedDate = $this->resolveDate($date);
        $methodId = $method ?? (int) config('aladhan.default_method', 19);
        $schoolId = $school ?? (int) config('aladhan.default_school', 0);

        $cleanAddr = md5(strtolower(trim($address)));

        $cacheKey = sprintf(
            'prayer:addr:%s:%s:%d:%d:%s',
            $cleanAddr,
            $parsedDate->format('Y-m-d'),
            $methodId,
            $schoolId,
            $tune ?? 'none'
        );

        return $this->rememberWithFallback($cacheKey, function () use ($parsedDate, $address, $methodId, $schoolId, $tune) {
            $dateParam = $parsedDate->format('d-m-Y');
            $params = [
                'address' => $address,
                'method' => $methodId,
                'school' => $schoolId,
            ];
            if (!empty($tune)) {
                $params['tune'] = $tune;
            }

            $response = $this->client->get("timingsByAddress/{$dateParam}", $params);

            if (!$response->successful()) {
                Log::error("AlAdhan API error in timingsByAddress: {$response->status()}");
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $json = $response->json();
            if (!isset($json['data'])) {
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $rawMeta = $json['data']['meta'] ?? [];
            $lat = (float) ($rawMeta['latitude'] ?? 0.0);
            $lng = (float) ($rawMeta['longitude'] ?? 0.0);

            return $this->normalizeDailyData($json['data'], $lat, $lng, $parsedDate->format('Y-m-d'));
        });
    }

    /**
     * Get monthly prayer calendar for coordinates.
     */
    public function getMonthlyCalendar(
        float $latitude,
        float $longitude,
        int $month,
        int $year,
        ?int $method = null,
        ?int $school = null,
        ?string $tune = null
    ): array {
        $methodId = $method ?? (int) config('aladhan.default_method', 19);
        $schoolId = $school ?? (int) config('aladhan.default_school', 0);

        $cacheKey = sprintf(
            'prayer:month:%04d-%02d:%s:%s:%d:%d:%s',
            $year,
            $month,
            number_format($latitude, 4, '.', ''),
            number_format($longitude, 4, '.', ''),
            $methodId,
            $schoolId,
            $tune ?? 'none'
        );

        return $this->rememberWithFallback($cacheKey, function () use ($year, $month, $latitude, $longitude, $methodId, $schoolId, $tune) {
            $params = [
                'latitude' => $latitude,
                'longitude' => $longitude,
                'method' => $methodId,
                'school' => $schoolId,
            ];
            if (!empty($tune)) {
                $params['tune'] = $tune;
            }

            $response = $this->client->get("calendar/{$year}/{$month}", $params);

            if (!$response->successful()) {
                Log::error("AlAdhan API error in calendar: {$response->status()}");
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $json = $response->json();
            if (!isset($json['data']) || !is_array($json['data'])) {
                throw new \RuntimeException('ALADHAN_INVALID_RESPONSE', 502);
            }

            $days = [];
            foreach ($json['data'] as $dayData) {
                $gregorianDate = $dayData['date']['gregorian']['date'] ?? ''; // e.g. "17-09-2026"
                $formattedDate = '';
                if ($gregorianDate) {
                    try {
                        $formattedDate = Carbon::createFromFormat('d-m-Y', $gregorianDate)->format('Y-m-d');
                    } catch (\Throwable) {
                        $formattedDate = $gregorianDate;
                    }
                }

                $rawTimings = $dayData['timings'] ?? [];
                $normalizedTimings = $this->normalizeTimings($rawTimings);

                $hijri = null;
                if (isset($dayData['date']['hijri'])) {
                    $h = $dayData['date']['hijri'];
                    $hijri = [
                        'day' => (string) ($h['day'] ?? ''),
                        'month' => (int) ($h['month']['number'] ?? 0),
                        'monthName' => (string) ($h['month']['en'] ?? ''),
                        'monthNameAr' => (string) ($h['month']['ar'] ?? ''),
                        'year' => (string) ($h['year'] ?? '')
                    ];
                }

                $days[] = [
                    'date' => $formattedDate,
                    'timings' => $normalizedTimings,
                    'hijri' => $hijri
                ];
            }

            return [
                'month' => $month,
                'year' => $year,
                'location' => [
                    'latitude' => $latitude,
                    'longitude' => $longitude,
                ],
                'days' => $days
            ];
        });
    }

    /**
     * Cache helper with stale cache fallback if upstream AlAdhan is down.
     */
    protected function rememberWithFallback(string $key, \Closure $fetcher): array
    {
        if (!$this->cacheEnabled) {
            $data = $fetcher();
            return [
                'data' => $data,
                'meta' => ['provider' => 'AlAdhan', 'cached' => false, 'stale' => false]
            ];
        }

        // Try to read fresh from cache
        if (Cache::has($key)) {
            $cached = Cache::get($key);
            return [
                'data' => $cached,
                'meta' => ['provider' => 'AlAdhan', 'cached' => true, 'stale' => false]
            ];
        }

        try {
            $freshData = $fetcher();
            Cache::put($key, $freshData, $this->cacheTtl);
            // Also store a long-term backup cache in case upstream goes down later
            Cache::put("stale:{$key}", $freshData, $this->cacheTtl * 7);

            return [
                'data' => $freshData,
                'meta' => ['provider' => 'AlAdhan', 'cached' => false, 'stale' => false]
            ];
        } catch (\Throwable $e) {
            Log::warning("Upstream call failed, checking for stale cache for key {$key}: " . $e->getMessage());

            if (Cache::has("stale:{$key}")) {
                $stale = Cache::get("stale:{$key}");
                return [
                    'data' => $stale,
                    'meta' => ['provider' => 'AlAdhan', 'cached' => true, 'stale' => true]
                ];
            }

            throw $e;
        }
    }

    /**
     * Normalize timings & metadata from AlAdhan daily response.
     */
    protected function normalizeDailyData(array $rawData, float $fallbackLat, float $fallbackLng, string $dateStr): array
    {
        $rawTimings = $rawData['timings'] ?? [];
        $rawMeta = $rawData['meta'] ?? [];
        $rawDate = $rawData['date'] ?? [];

        $timings = $this->normalizeTimings($rawTimings);

        $methodId = (int) ($rawMeta['method']['id'] ?? config('aladhan.default_method', 19));
        $methodName = (string) ($rawMeta['method']['name'] ?? 'Custom');

        $timezone = (string) ($rawMeta['timezone'] ?? 'UTC');

        $lat = isset($rawMeta['latitude']) ? (float) $rawMeta['latitude'] : $fallbackLat;
        $lng = isset($rawMeta['longitude']) ? (float) $rawMeta['longitude'] : $fallbackLng;

        $hijriData = null;
        if (isset($rawDate['hijri'])) {
            $h = $rawDate['hijri'];
            $hijriData = [
                'day' => (string) ($h['day'] ?? ''),
                'month' => (int) ($h['month']['number'] ?? 0),
                'monthName' => (string) ($h['month']['en'] ?? ''),
                'monthNameAr' => (string) ($h['month']['ar'] ?? ''),
                'year' => (string) ($h['year'] ?? '')
            ];
        }

        // Calculate next prayer based on current timezone time
        $nextPrayer = $this->determineNextPrayer($timings, $timezone);

        return [
            'date' => $dateStr,
            'location' => [
                'latitude' => $lat,
                'longitude' => $lng
            ],
            'timezone' => $timezone,
            'method' => [
                'id' => $methodId,
                'name' => $methodName
            ],
            'timings' => $timings,
            'hijri' => $hijriData,
            'nextPrayer' => $nextPrayer
        ];
    }

    /**
     * Strip time-zone suffixes like "05:01 (+01)" -> "05:01".
     */
    public function cleanTimeString(?string $raw): string
    {
        if (empty($raw)) {
            return '';
        }
        $parts = explode(' ', trim($raw));
        return $parts[0] ?? '';
    }

    /**
     * Extract clean 24h timings.
     */
    protected function normalizeTimings(array $raw): array
    {
        return [
            'fajr' => $this->cleanTimeString($raw['Fajr'] ?? $raw['fajr'] ?? null),
            'sunrise' => $this->cleanTimeString($raw['Sunrise'] ?? $raw['sunrise'] ?? null),
            'dhuhr' => $this->cleanTimeString($raw['Dhuhr'] ?? $raw['dhuhr'] ?? null),
            'asr' => $this->cleanTimeString($raw['Asr'] ?? $raw['asr'] ?? null),
            'sunset' => $this->cleanTimeString($raw['Sunset'] ?? $raw['sunset'] ?? null),
            'maghrib' => $this->cleanTimeString($raw['Maghrib'] ?? $raw['maghrib'] ?? null),
            'isha' => $this->cleanTimeString($raw['Isha'] ?? $raw['isha'] ?? null),
            'imsak' => $this->cleanTimeString($raw['Imsak'] ?? $raw['imsak'] ?? null),
            'midnight' => $this->cleanTimeString($raw['Midnight'] ?? $raw['midnight'] ?? null),
        ];
    }

    /**
     * Compute next prayer for convenience (optional in PRD, computed using timezone).
     */
    protected function determineNextPrayer(array $timings, string $timezone): ?array
    {
        try {
            $now = Carbon::now($timezone);
            $order = [
                'Fajr' => $timings['fajr'],
                'Sunrise' => $timings['sunrise'],
                'Dhuhr' => $timings['dhuhr'],
                'Asr' => $timings['asr'],
                'Maghrib' => $timings['maghrib'],
                'Isha' => $timings['isha']
            ];

            foreach ($order as $name => $timeStr) {
                if (empty($timeStr)) continue;
                $parts = explode(':', $timeStr);
                if (count($parts) >= 2) {
                    $prayerTime = Carbon::now($timezone)->setTime((int)$parts[0], (int)$parts[1], 0);
                    if ($now->lt($prayerTime)) {
                        return [
                            'name' => $name,
                            'time' => $timeStr
                        ];
                    }
                }
            }

            // If past Isha, next is tomorrow's Fajr
            return [
                'name' => 'Fajr',
                'time' => $timings['fajr']
            ];
        } catch (\Throwable) {
            return null;
        }
    }

    protected function resolveDate(?string $date): Carbon
    {
        if (empty($date)) {
            return Carbon::today();
        }
        try {
            return Carbon::createFromFormat('Y-m-d', $date)->startOfDay();
        } catch (\Throwable) {
            return Carbon::today();
        }
    }
}
