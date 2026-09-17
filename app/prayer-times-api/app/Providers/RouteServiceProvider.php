<?php

namespace App\Providers;

use Illuminate\Cache\RateLimiting\Limit;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\RateLimiter;
use Illuminate\Support\ServiceProvider;

class RouteServiceProvider extends ServiceProvider
{
    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        $this->configureRateLimiting();
    }

    /**
     * Configure rate limiters for the application.
     * PRD Section 28: Rate limiting by IP (default 60 req/min/IP).
     */
    protected function configureRateLimiting(): void
    {
        RateLimiter::for('prayer-api', function (Request $request) {
            $limitPerMinute = (int) config('aladhan.rate_limit', 60);

            return Limit::perMinute($limitPerMinute)->by($request->ip())->response(function () {
                return response()->json([
                    'success' => false,
                    'error' => [
                        'code' => 'RATE_LIMITED',
                        'message' => 'Too many requests. You have exceeded the allowable rate limit of requests per minute.'
                    ]
                ], 429);
            });
        });
    }
}
