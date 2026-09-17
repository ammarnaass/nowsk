<?php

namespace Tests\Unit;

use App\Services\AlAdhan\AlAdhanClient;
use App\Services\Calendar\ConversionService;
use Carbon\Carbon;
use Mockery;
use Tests\TestCase;

class CalendarConversionUnitTest extends TestCase
{
    protected ConversionService $service;

    protected function setUp(): void
    {
        parent::setUp();
        $mockClient = Mockery::mock(AlAdhanClient::class);
        $this->service = new ConversionService($mockClient);
    }

    public function test_arabic_weekdays_mapping(): void
    {
        $this->assertEquals('الخميس', $this->service->getArabicWeekday(Carbon::THURSDAY));
        $this->assertEquals('الجمعة', $this->service->getArabicWeekday(Carbon::FRIDAY));
        $this->assertEquals('السبت', $this->service->getArabicWeekday(Carbon::SATURDAY));
        $this->assertEquals('الأحد', $this->service->getArabicWeekday(Carbon::SUNDAY));
    }

    public function test_arabic_weekday_from_name(): void
    {
        $this->assertEquals('الخميس', $this->service->getArabicWeekdayFromName('Thursday'));
        $this->assertEquals('الجمعة', $this->service->getArabicWeekdayFromName('Friday'));
    }
}
