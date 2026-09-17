<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class PrayerTimesResource extends JsonResource
{
    /**
     * Transform the resource into an array.
     */
    public function toArray(Request $request): array
    {
        return [
            'date' => $this->resource['date'] ?? null,
            'location' => $this->resource['location'] ?? null,
            'timezone' => $this->resource['timezone'] ?? 'UTC',
            'method' => $this->resource['method'] ?? null,
            'timings' => $this->resource['timings'] ?? null,
            'hijri' => $this->resource['hijri'] ?? null,
            'nextPrayer' => $this->resource['nextPrayer'] ?? null,
        ];
    }
}
