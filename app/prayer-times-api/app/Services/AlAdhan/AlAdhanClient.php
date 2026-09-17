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
     * Perform GET request against AlAdhan API.
     *
     * @param string $endpoint e.g. "timings/17-09-2026"
     * @param array $params Query string parameters
     * @return Response
     */
    public function get(string $endpoint, array $params = []): Response
    {
        $url = $this->baseUrl . '/' . ltrim($endpoint, '/');

        return Http::timeout($this->timeout)
            ->acceptJson()
            ->get($url, $params);
    }

    /**
     * Check upstream AlAdhan health by requesting methods list.
     *
     * @return bool
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
