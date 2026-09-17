<?php

namespace Tests\Feature;

use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Tests\TestCase;

class CalendarApiTest extends TestCase
{
    protected function setUp(): void
    {
        parent::setUp();
        Cache::flush();
    }

    public function test_gregorian_to_hijri_validation_fails_on_invalid_date(): void
    {
        $response = $this->getJson('/api/v1/calendar/gregorian-to-hijri?date=2026-99-99');

        $response->assertStatus(422)
            ->assertJson([
                'success' => false,
                'error' => [
                    'code' => 'INVALID_GREGORIAN_DATE'
                ]
            ]);
    }

    public function test_hijri_to_gregorian_validation_fails_on_invalid_format(): void
    {
        $response = $this->getJson('/api/v1/calendar/hijri-to-gregorian?date=invalid-date');

        $response->assertStatus(422)
            ->assertJson([
                'success' => false,
                'error' => [
                    'code' => 'INVALID_HIJRI_DATE'
                ]
            ]);
    }

    public function test_gregorian_to_hijri_successful_conversion_with_mock(): void
    {
        Http::fake([
            'https://api.aladhan.com/v1/gToH/17-09-2026*' => Http::response([
                'code' => 200,
                'status' => 'OK',
                'data' => [
                    'hijri' => [
                        'date' => '25-03-1448',
                        'day' => '25',
                        'month' => [
                            'number' => 3,
                            'en' => 'Rabīʿ al-awwal',
                            'ar' => 'رَبِيع الأَوّل'
                        ],
                        'year' => '1448',
                        'holidays' => []
                    ],
                    'gregorian' => [
                        'date' => '17-09-2026',
                        'day' => '17',
                        'month' => [
                            'number' => 9,
                            'en' => 'September'
                        ],
                        'year' => '2026',
                        'weekday' => [
                            'en' => 'Thursday'
                        ]
                    ]
                ]
            ], 200)
        ]);

        $response = $this->getJson('/api/v1/calendar/gregorian-to-hijri?date=2026-09-17');

        $response->assertStatus(200)
            ->assertJson([
                'success' => true,
                'data' => [
                    'gregorian' => [
                        'date' => '2026-09-17',
                        'day' => 17,
                        'month' => 9,
                        'year' => 2026
                    ],
                    'hijri' => [
                        'date' => '25-03-1448',
                        'day' => 25,
                        'month' => 3,
                        'year' => 1448
                    ]
                ],
                'meta' => [
                    'provider' => 'AlAdhan',
                    'cached' => false
                ]
            ]);
    }

    public function test_hijri_to_gregorian_successful_conversion_with_mock(): void
    {
        Http::fake([
            'https://api.aladhan.com/v1/hToG/25-03-1448*' => Http::response([
                'code' => 200,
                'status' => 'OK',
                'data' => [
                    'hijri' => [
                        'date' => '25-03-1448',
                        'day' => '25',
                        'month' => [
                            'number' => 3,
                            'en' => 'Rabīʿ al-awwal',
                            'ar' => 'رَبِيع الأَوّل'
                        ],
                        'year' => '1448',
                        'holidays' => []
                    ],
                    'gregorian' => [
                        'date' => '17-09-2026',
                        'day' => '17',
                        'month' => [
                            'number' => 9,
                            'en' => 'September'
                        ],
                        'year' => '2026',
                        'weekday' => [
                            'en' => 'Thursday'
                        ]
                    ]
                ]
            ], 200)
        ]);

        $response = $this->getJson('/api/v1/calendar/hijri-to-gregorian?date=25-03-1448');

        $response->assertStatus(200)
            ->assertJson([
                'success' => true,
                'data' => [
                    'hijri' => [
                        'date' => '25-03-1448',
                        'day' => 25,
                        'month' => 3,
                        'year' => 1448
                    ],
                    'gregorian' => [
                        'date' => '2026-09-17',
                        'day' => 17,
                        'month' => 9,
                        'year' => 2026
                    ]
                ]
            ]);
    }

    public function test_health_check_endpoint(): void
    {
        Http::fake([
            'https://api.aladhan.com/v1/methods' => Http::response([], 200)
        ]);

        $response = $this->getJson('/api/health');

        $response->assertStatus(200)
            ->assertJson([
                'status' => 'ok',
                'service' => 'Islamic Calendar API',
                'version' => '1.0.0'
            ]);
    }
}
