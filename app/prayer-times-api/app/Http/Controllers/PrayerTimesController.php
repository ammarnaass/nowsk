<?php

namespace App\Http\Controllers;

use App\Http\Requests\AddressPrayerTimesRequest;
use App\Http\Requests\CityPrayerTimesRequest;
use App\Http\Requests\MonthlyPrayerTimesRequest;
use App\Http\Requests\PrayerTimesRequest;
use App\Http\Resources\PrayerTimesResource;
use App\Services\AlAdhan\MethodsService;
use App\Services\AlAdhan\PrayerTimesService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Log;

class PrayerTimesController extends Controller
{
    protected PrayerTimesService $prayerTimesService;
    protected MethodsService $methodsService;

    public function __construct(
        PrayerTimesService $prayerTimesService,
        MethodsService $methodsService
    ) {
        $this->prayerTimesService = $prayerTimesService;
        $this->methodsService = $methodsService;
    }

    /**
     * GET /api/v1/prayer-times
     * Daily prayer times by latitude & longitude.
     */
    public function daily(PrayerTimesRequest $request): JsonResponse
    {
        try {
            $result = $this->prayerTimesService->getTimingsByCoordinates(
                latitude: (float) $request->input('latitude'),
                longitude: (float) $request->input('longitude'),
                date: $request->input('date'),
                method: $request->filled('method') ? (int) $request->input('method') : null,
                school: $request->filled('school') ? (int) $request->input('school') : null,
                tune: $request->input('tune')
            );

            return response()->json([
                'success' => true,
                'data' => (new PrayerTimesResource($result['data']))->resolve(),
                'meta' => $result['meta']
            ], 200);
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/prayer-times/city
     * Daily prayer times by city & country.
     */
    public function byCity(CityPrayerTimesRequest $request): JsonResponse
    {
        try {
            $result = $this->prayerTimesService->getTimingsByCity(
                city: $request->input('city'),
                country: $request->input('country'),
                date: $request->input('date'),
                method: $request->filled('method') ? (int) $request->input('method') : null,
                school: $request->filled('school') ? (int) $request->input('school') : null,
                tune: $request->input('tune')
            );

            return response()->json([
                'success' => true,
                'data' => (new PrayerTimesResource($result['data']))->resolve(),
                'meta' => $result['meta']
            ], 200);
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/prayer-times/address
     * Daily prayer times by custom address string.
     */
    public function byAddress(AddressPrayerTimesRequest $request): JsonResponse
    {
        try {
            $result = $this->prayerTimesService->getTimingsByAddress(
                address: $request->input('address'),
                date: $request->input('date'),
                method: $request->filled('method') ? (int) $request->input('method') : null,
                school: $request->filled('school') ? (int) $request->input('school') : null,
                tune: $request->input('tune')
            );

            return response()->json([
                'success' => true,
                'data' => (new PrayerTimesResource($result['data']))->resolve(),
                'meta' => $result['meta']
            ], 200);
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/prayer-times/monthly
     * Monthly prayer times calendar by coordinates.
     */
    public function monthly(MonthlyPrayerTimesRequest $request): JsonResponse
    {
        try {
            $result = $this->prayerTimesService->getMonthlyCalendar(
                latitude: (float) $request->input('latitude'),
                longitude: (float) $request->input('longitude'),
                month: (int) $request->input('month'),
                year: (int) $request->input('year'),
                method: $request->filled('method') ? (int) $request->input('method') : null,
                school: $request->filled('school') ? (int) $request->input('school') : null,
                tune: $request->input('tune')
            );

            return response()->json([
                'success' => true,
                'data' => $result['data'],
                'meta' => $result['meta']
            ], 200);
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * GET /api/v1/methods
     * List supported calculation methods.
     */
    public function methods(): JsonResponse
    {
        try {
            $methods = $this->methodsService->getMethods();

            return response()->json([
                'success' => true,
                'data' => $methods,
                'meta' => [
                    'provider' => 'AlAdhan',
                    'count' => count($methods)
                ]
            ], 200);
        } catch (\Throwable $e) {
            return $this->handleException($e);
        }
    }

    /**
     * Common error handler mapping to standard PRD error response.
     */
    protected function handleException(\Throwable $e): JsonResponse
    {
        $message = $e->getMessage();
        $code = $e->getCode() ?: 500;

        Log::error("API Error: {$message}", ['trace' => $e->getTraceAsString()]);

        if (str_contains($message, 'ALADHAN_INVALID_RESPONSE')) {
            return response()->json([
                'success' => false,
                'error' => [
                    'code' => 'ALADHAN_INVALID_RESPONSE',
                    'message' => 'Invalid or failed response from AlAdhan upstream service.'
                ]
            ], 502);
        }

        if (str_contains($message, 'timed out') || str_contains($message, 'Connection timed out')) {
            return response()->json([
                'success' => false,
                'error' => [
                    'code' => 'ALADHAN_TIMEOUT',
                    'message' => 'Upstream service timed out. Please try again later.'
                ]
            ], 504);
        }

        return response()->json([
            'success' => false,
            'error' => [
                'code' => 'INTERNAL_ERROR',
                'message' => 'An unexpected internal error occurred.'
            ]
        ], ($code >= 400 && $code < 600) ? $code : 500);
    }
}
