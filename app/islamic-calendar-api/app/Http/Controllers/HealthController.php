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
    public function check(): JsonResponse
    {
        $redisHealthy = true;
        try {
            Cache::put('health_ping', 'pong', 5);
            $redisHealthy = Cache::get('health_ping') === 'pong';
        } catch (\Throwable $e) {
            $redisHealthy = false;
        }

        $upstreamHealthy = $this->client->isHealthy();

        $overallStatus = ($redisHealthy && $upstreamHealthy) ? 'ok' : 'degraded';

        return response()->json([
            'status' => $overallStatus,
            'service' => 'Islamic Calendar API',
            'version' => '1.0.0',
            'dependencies' => [
                'cache' => $redisHealthy ? 'up' : 'down',
                'aladhan_upstream' => $upstreamHealthy ? 'up' : 'down'
            ]
        ], $overallStatus === 'ok' ? 200 : 200);
    }
}
