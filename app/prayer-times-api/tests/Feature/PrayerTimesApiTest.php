<?php

namespace Tests\Feature;

use Illuminate\Support\Facades\Http;
use Tests\TestCase;

class PrayerTimesApiTest extends TestCase
{
    /**
     * Test Health endpoint.
     */
    public function test_health_check_endpoint(): void
    {
        $response = $this->getJson('/api/health');

        $response->assertStatus(200)
            ->assertJson([
                'status' => 'ok',
                'service' => 'Prayer Times API',
                'version' => '1.0.0'
            ]);
    }

    /**
     * Test Validation on missing coordinates.
     */
    public function test_daily_prayer_times_requires_coordinates(): void
    {
        $response = $this->getJson('/api/v1/prayer-times');

        $response->assertStatus(422)
            ->assertJsonStructure([
                'success',
                'error' => [
                    'code',
                    'message'
                ]
            ])
            ->assertJson([
                'success' => false,
                'error' => [
                    'code' => 'INVALID_LATITUDE'
                ]
            ]);
    }

    /**
     * Test Validation on out-of-range latitude.
     */
    public function test_daily_prayer_times_validates_latitude_bounds(): void
    {
        $response = $this->getJson('/api/v1/prayer-times?latitude=105.5&longitude=4.5');

        $response->assertStatus(422)
            ->assertJson([
                'success' => false,
                'error' => [
                    'code' => 'INVALID_LATITUDE'
                ]
            ]);
    }

    /**
     * Test successful daily prayer times retrieval with mocked upstream AlAdhan API.
     */
    public function test_daily_prayer_times_success(): void
    {
        Http::fake([
            'https://api.aladhan.com/v1/timings/*' => Http::response([
                'code' => 200,
                'status' => 'OK',
                'data' => [
                    'timings' => [
                        'Fajr' => '05:01 (+01)',
                        'Sunrise' => '06:29 (+01)',
                        'Dhuhr' => '12:34 (+01)',
                        'Asr' => '16:08 (+01)',
                        'Sunset' => '18:42 (+01)',
                        'Maghrib' => '18:42 (+01)',
                        'Isha' => '20:03 (+01)',
                        'Imsak' => '04:51 (+01)',
                        'Midnight' => '00:35 (+01)',
                    ],
                    'date' => [
                        'readable' => '17 Sep 2026',
                        'timestamp' => '1789700000',
                        'hijri' => [
                            'date' => '25-03-1448',
                            'day' => '25',
                            'month' => [
                                'number' => 3,
                                'en' => 'Rabi al-awwal',
                                'ar' => 'ربيع الأول'
                            ],
                            'year' => '1448'
                        ]
                    ],
                    'meta' => [
                        'latitude' => 35.7058,
                        'longitude' => 4.5419,
                        'timezone' => 'Africa/Algiers',
                        'method' => [
                            'id' => 19,
                            'name' => 'Ministry of Religious Affairs and Wakfs, Algeria'
                        ]
                    ]
                ]
            ], 200)
        ]);

        $response = $this->getJson('/api/v1/prayer-times?latitude=35.7058&longitude=4.5419&date=2026-09-17&method=19&school=0');

        $response->assertStatus(200)
            ->assertJson([
                'success' => true,
                'data' => [
                    'date' => '2026-09-17',
                    'location' => [
                        'latitude' => 35.7058,
                        'longitude' => 4.5419
                    ],
                    'timings' => [
                        'fajr' => '05:01',
                        'sunrise' => '06:29',
                        'dhuhr' => '12:34',
                        'asr' => '16:08',
                        'maghrib' => '18:42',
                        'isha' => '20:03',
                        'imsak' => '04:51',
                        'midnight' => '00:35'
                    ],
                    'hijri' => [
                        'day' => '25',
                        'month' => 3,
                        'monthName' => 'Rabi al-awwal',
                        'year' => '1448'
                    ]
                ],
                'meta' => [
                    'provider' => 'AlAdhan'
                ]
            ]);
    }

    /**
     * Test Calculation Methods list endpoint.
     */
    public function test_methods_endpoint_success(): void
    {
        Http::fake([
            'https://api.aladhan.com/v1/methods' => Http::response([
                'code' => 200,
                'status' => 'OK',
                'data' => [
                    'MWL' => [
                        'id' => 3,
                        'name' => 'Muslim World League'
                    ],
                    'EGYPT' => [
                        'id' => 5,
                        'name' => 'Egyptian General Authority of Survey'
                    ]
                ]
            ], 200)
        ]);

        $response = $this->getJson('/api/v1/methods');

        $response->assertStatus(200)
            ->assertJson([
                'success' => true,
                'data' => [
                    ['id' => 3, 'name' => 'Muslim World League'],
                    ['id' => 5, 'name' => 'Egyptian General Authority of Survey']
                ]
            ]);
    }
}
