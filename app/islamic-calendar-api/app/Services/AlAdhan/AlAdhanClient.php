<?php

namespace App\Services\AlAdhan;

use Illuminate\Http\Client\Response;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;

class AlAdhanClient
{
    protected string $baseUrl;
    protected int $timeout;

    public function __construct()
    {
        $this->baseUrl = rtrim(config('aladhan.base_url', 'https://api.aladhan.com/v1'), '/');
        $this->timeout = (int) config('aladhan.timeout', 10);
    }

    /**
     * Perform GET request against AlAdhan API endpoint.
     */
    public function get(string $endpoint, array $params = []): Response
    {
        $url = $this->baseUrl . '/' . ltrim($endpoint, '/');

        return Http::timeout($this->timeout)
            ->acceptJson()
            ->get($url, $params);
    }

    /**
     * Health check verifying upstream reachability.
     */
    public function isHealthy(): bool
    {
        try {
            $response = Http::timeout(3)->get($this->baseUrl . '/methods');
            return $response->successful();
        } catch (\Throwable $e) {
            Log::warning('AlAdhan upstream health check failed: ' . $e->getMessage());
            return false;
        }
    }
}
