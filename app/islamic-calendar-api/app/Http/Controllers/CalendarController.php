<?php

namespace App\Http\Controllers;

use App\Http\Requests\GregorianCalendarRequest;
use App\Http\Requests\GregorianToHijriRequest;
use App\Http\Requests\HijriCalendarRequest;
use App\Http\Requests\HijriToGregorianRequest;
use App\Http\Requests\HolidaysRequest;
use App\Http\Requests\TodayRequest;
use App\Http\Resources\CalendarResource;
use App\Services\AlAdhan\CalendarService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class CalendarController extends Controller
{
    protected CalendarService $calendarService;

    public function __construct(CalendarService $calendarService)
    {
        $this->calendarService = $calendarService;
    }

    /**
     * GET /api/v1/calendar/today
     */
    public function today(TodayRequest $request): JsonResponse
    {
        try {
            $timezone = $request->input('timezone');
            $country = $request->input('country');
            $method = $request->input('method');
            $adjustment = (int) $request->input('adjustment', 0);

            $result = $this->calendarService->getToday($timezone, $country, $method, $adjustment);

            return response()->json((new CalendarResource($result))->resolve());
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/calendar/gregorian-to-hijri
     */
    public function gregorianToHijri(GregorianToHijriRequest $request): JsonResponse
    {
        try {
            $date = $request->input('date');
            $country = $request->input('country');
            $method = $request->input('method');
            $adjustment = (int) $request->input('adjustment', 0);

            $result = $this->calendarService->convertGregorianToHijri($date, $country, $method, $adjustment);

            return response()->json((new CalendarResource($result))->resolve());
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/calendar/hijri-to-gregorian
     */
    public function hijriToGregorian(HijriToGregorianRequest $request): JsonResponse
    {
        try {
            $date = $request->input('date');
            $country = $request->input('country');
            $method = $request->input('method');
            $adjustment = (int) $request->input('adjustment', 0);

            $result = $this->calendarService->convertHijriToGregorian($date, $country, $method, $adjustment);

            return response()->json((new CalendarResource($result))->resolve());
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/calendar/gregorian
     */
    public function gregorian(GregorianCalendarRequest $request): JsonResponse
    {
        try {
            $month = (int) $request->input('month');
            $year = (int) $request->input('year');
            $country = $request->input('country');
            $method = $request->input('method');
            $adjustment = (int) $request->input('adjustment', 0);

            $result = $this->calendarService->getGregorianCalendar($month, $year, $country, $method, $adjustment);

            return response()->json((new CalendarResource($result))->resolve());
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/calendar/hijri
     */
    public function hijri(HijriCalendarRequest $request): JsonResponse
    {
        try {
            $month = (int) $request->input('month');
            $year = (int) $request->input('year');
            $country = $request->input('country');
            $method = $request->input('method');
            $adjustment = (int) $request->input('adjustment', 0);

            $result = $this->calendarService->getHijriCalendar($month, $year, $country, $method, $adjustment);

            return response()->json((new CalendarResource($result))->resolve());
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/calendar/holidays
     */
    public function holidays(HolidaysRequest $request): JsonResponse
    {
        try {
            $year = (int) $request->input('year');
            $country = $request->input('country');
            $method = $request->input('method');

            $result = $this->calendarService->getHolidays($year, $country, $method);

            return response()->json((new CalendarResource($result))->resolve());
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    protected function handleException(\Throwable $e): JsonResponse
    {
        Log::error('CalendarController error: ' . $e->getMessage());

        $message = $e->getMessage();
        $statusCode = 500;
        $errorCode = 'INTERNAL_ERROR';

        if (str_contains($message, 'ALADHAN_INVALID_RESPONSE')) {
            $errorCode = 'ALADHAN_INVALID_RESPONSE';
            $statusCode = 502;
            $message = 'Received invalid response from upstream AlAdhan provider.';
        } elseif (str_contains($message, 'ALADHAN_TIMEOUT')) {
            $errorCode = 'ALADHAN_TIMEOUT';
            $statusCode = 504;
            $message = 'Upstream AlAdhan provider request timed out.';
        } elseif (str_contains($message, 'ALADHAN_UNAVAILABLE')) {
            $errorCode = 'ALADHAN_UNAVAILABLE';
            $statusCode = 503;
            $message = 'Islamic Calendar provider is temporarily unavailable and no cached data exists.';
        }

        return response()->json([
            'success' => false,
            'error' => [
                'code' => $errorCode,
                'message' => $message
            ]
        ], $statusCode);
    }
}
