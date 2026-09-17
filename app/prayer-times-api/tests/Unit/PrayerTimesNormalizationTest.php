<?php

namespace Tests\Unit;

use App\Services\AlAdhan\AlAdhanClient;
use App\Services\AlAdhan\PrayerTimesService;
use PHPUnit\Framework\TestCase;

class PrayerTimesNormalizationTest extends TestCase
{
    /**
     * Test cleaning time strings with timezone offsets like "05:01 (+01)" -> "05:01"
     */
    public function test_clean_time_string_removes_timezone_suffix(): void
    {
        $mockClient = $this->createMock(AlAdhanClient::class);
        $service = new PrayerTimesService($mockClient);

        $this->assertEquals('05:01', $service->cleanTimeString('05:01 (+01)'));
        $this->assertEquals('18:42', $service->cleanTimeString('18:42 (CET)'));
        $this->assertEquals('12:34', $service->cleanTimeString('12:34'));
        $this->assertEquals('', $service->cleanTimeString(null));
        $this->assertEquals('', $service->cleanTimeString(''));
    }
}
