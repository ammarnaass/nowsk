<?php

use App\Http\Controllers\HealthController;
use App\Http\Controllers\PrayerTimesController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes - Prayer Times Service
|--------------------------------------------------------------------------
*/

// Health Check Endpoint (PRD Section 45)
Route::get('/health', [HealthController::class, 'health']);

// API Version 1 Group
Route::prefix('v1')->middleware(['throttle:prayer-api'])->group(function () {

    // Daily prayer times by GPS coordinates (PRD Section 10)
    Route::get('/prayer-times', [PrayerTimesController::class, 'daily']);

    // Daily prayer times by City & Country (PRD Section 12)
    Route::get('/prayer-times/city', [PrayerTimesController::class, 'byCity']);

    // Daily prayer times by Address (PRD Section 13)
    Route::get('/prayer-times/address', [PrayerTimesController::class, 'byAddress']);

    // Monthly prayer times calendar (PRD Section 14)
    Route::get('/prayer-times/monthly', [PrayerTimesController::class, 'monthly']);

    // Supported calculation methods (PRD Section 15)
    Route::get('/methods', [PrayerTimesController::class, 'methods']);
});
