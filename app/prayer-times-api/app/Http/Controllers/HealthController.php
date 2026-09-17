<?php

namespace App\Http\Controllers;

use App\Services\AlAdhan\AlAdhanClient;
use Illuminate\Http\JsonResponse;
use Illuminate\Support\Facades\Cache;

class HealthController extends Controller
{
    protected AlAdhanClient $client;

    public function __construct(AlAdhanClient $client)
    {
        $this->client = $client;
    }

    /**
     * GET /api/health
     */
    public function health(): JsonResponse
    {
        $redisOk = false;
        try {
            Cache::put('health_test_key', '1', 5);
            $redisOk = (Cache::get('health_test_key') === '1');
        } catch (\Throwable) {
            $redisOk = false;
        }

        return response()->json([
            'status' => 'ok',
            'service' => 'Prayer Times API',
            'version' => '1.0.0',
            'cache' => $redisOk ? 'connected' : 'degraded',
            'time' => now()->toIso8601String()
        ], 200);
    }
}
