<?php

namespace App\Http\Resources;

use Illuminate\Http\Request;
use Illuminate\Http\Resources\Json\JsonResource;

class CalendarResource extends JsonResource
{
    /**
     * Transform the resource into an array.
     */
    public function toArray(Request $request): array
    {
        $response = [
            'success' => true,
            'data' => $this->resource['data'] ?? $this->resource
        ];

        if (isset($this->resource['meta'])) {
            $response['meta'] = $this->resource['meta'];
        }

        return $response;
    }
}
