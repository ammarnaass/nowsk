<?php

use App\Http\Controllers\CalendarController;
use App\Http\Controllers\HealthController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Prefix: /api/v1/calendar and /api/health
|
*/

Route::get('/health', [HealthController::class, 'check']);

Route::prefix('v1/calendar')
    ->middleware('throttle:calendar')
    ->group(function () {
        Route::get('/today', [CalendarController::class, 'today']);
        Route::get('/gregorian-to-hijri', [CalendarController::class, 'gregorianToHijri']);
        Route::get('/hijri-to-gregorian', [CalendarController::class, 'hijriToGregorian']);
        Route::get('/gregorian', [CalendarController::class, 'gregorian']);
        Route::get('/hijri', [CalendarController::class, 'hijri']);
        Route::get('/holidays', [CalendarController::class, 'holidays']);
    });
