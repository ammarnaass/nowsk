<?php

namespace App\Services\AlAdhan;

use App\Services\AlAdhan\AlAdhanClient;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;

class MethodsService
{
    protected AlAdhanClient $client;
    protected bool $cacheEnabled;
    protected int $cacheTtl;

    public function __construct(AlAdhanClient $client)
    {
        $this->client = $client;
        $this->cacheEnabled = config('aladhan.cache_enabled', true);
        $this->cacheTtl = config('aladhan.cache_ttl', 86400 * 7); // cache methods for 7 days
    }

    /**
     * Return list of available calculation methods.
     */
    public function getMethods(): array
    {
        $cacheKey = 'aladhan:methods';

        if ($this->cacheEnabled && Cache::has($cacheKey)) {
            return Cache::get($cacheKey);
        }

        try {
            $response = $this->client->get('methods');
            if ($response->successful()) {
                $json = $response->json();
                $rawMethods = $json['data'] ?? [];

                $methodsList = [];
                foreach ($rawMethods as $key => $methodData) {
                    $methodsList[] = [
                        'id' => (int) ($methodData['id'] ?? 0),
                        'name' => (string) ($methodData['name'] ?? $key),
                        'params' => $methodData['params'] ?? []
                    ];
                }

                // Sort by ID
                usort($methodsList, fn($a, $b) => $a['id'] <=> $b['id']);

                if ($this->cacheEnabled) {
                    Cache::put($cacheKey, $methodsList, $this->cacheTtl);
                }

                return $methodsList;
            }
        } catch (\Throwable $e) {
            Log::error('Failed to fetch methods from AlAdhan: ' . $e->getMessage());
        }

        // Return standard reliable fallback methods if upstream is unreachable
        return $this->getFallbackMethods();
    }

    protected function getFallbackMethods(): array
    {
        return [
            ['id' => 1, 'name' => 'University of Islamic Sciences, Karachi', 'params' => ['Fajr' => 18, 'Isha' => 18]],
            ['id' => 2, 'name' => 'Islamic Society of North America (ISNA)', 'params' => ['Fajr' => 15, 'Isha' => 15]],
            ['id' => 3, 'name' => 'Muslim World League', 'params' => ['Fajr' => 18, 'Isha' => 17]],
            ['id' => 4, 'name' => 'Umm Al-Qura University, Makkah', 'params' => ['Fajr' => 18.5, 'Isha' => '90 min']],
            ['id' => 5, 'name' => 'Egyptian General Authority of Survey', 'params' => ['Fajr' => 19.5, 'Isha' => 17.5]],
            ['id' => 7, 'name' => 'Institute of Geophysics, University of Tehran', 'params' => ['Fajr' => 17.7, 'Isha' => 14]],
            ['id' => 8, 'name' => 'Gulf Region', 'params' => ['Fajr' => 19.5, 'Isha' => '90 min']],
            ['id' => 9, 'name' => 'Kuwait', 'params' => ['Fajr' => 18, 'Isha' => 17.5]],
            ['id' => 10, 'name' => 'Qatar', 'params' => ['Fajr' => 18, 'Isha' => '90 min']],
            ['id' => 11, 'name' => 'Majlis Ugama Islam Singapura, Singapore', 'params' => ['Fajr' => 20, 'Isha' => 18]],
            ['id' => 12, 'name' => 'Union Des Organisations Islamiques De France', 'params' => ['Fajr' => 12, 'Isha' => 12]],
            ['id' => 13, 'name' => 'Diyanet İşleri Başkanlığı, Turkey', 'params' => ['Fajr' => 18, 'Isha' => 17]],
            ['id' => 14, 'name' => 'Spiritual Administration of Muslims of Russia', 'params' => ['Fajr' => 16, 'Isha' => 15]],
            ['id' => 19, 'name' => 'Ministry of Religious Affairs and Wakfs, Algeria', 'params' => ['Fajr' => 18, 'Isha' => 17]],
            ['id' => 20, 'name' => 'KEMENAG - Kementerian Agama Republik Indonesia', 'params' => ['Fajr' => 20, 'Isha' => 18]],
            ['id' => 21, 'name' => 'Morocco', 'params' => ['Fajr' => 19, 'Isha' => 17]],
            ['id' => 22, 'name' => 'Tunisia', 'params' => ['Fajr' => 18, 'Isha' => 18]],
        ];
    }
}
