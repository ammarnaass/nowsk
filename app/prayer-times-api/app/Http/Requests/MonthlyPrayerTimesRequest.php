<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class MonthlyPrayerTimesRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'latitude' => ['required', 'numeric', 'between:-90,90'],
            'longitude' => ['required', 'numeric', 'between:-180,180'],
            'month' => ['required', 'integer', 'between:1,12'],
            'year' => ['required', 'integer', 'between:1970,2100'],
            'method' => ['nullable', 'integer', 'min:0', 'max:50'],
            'school' => ['nullable', 'integer', 'in:0,1'],
            'tune' => ['nullable', 'regex:/^(-?\d+,){8}-?\d+$/'],
        ];
    }

    public function messages(): array
    {
        return [
            'latitude.required' => 'Latitude is required.',
            'latitude.between' => 'Latitude must be between -90 and 90.',
            'longitude.required' => 'Longitude is required.',
            'longitude.between' => 'Longitude must be between -180 and 180.',
            'month.required' => 'Month is required (1-12).',
            'month.between' => 'Month must be between 1 and 12.',
            'year.required' => 'Year is required.',
            'year.between' => 'Year must be a valid 4-digit year.',
        ];
    }

    protected function failedValidation(Validator $validator)
    {
        $errors = $validator->errors();
        $firstKey = $errors->keys()[0] ?? 'UNKNOWN';

        $errorCode = match ($firstKey) {
            'latitude' => 'INVALID_LATITUDE',
            'longitude' => 'INVALID_LONGITUDE',
            'month' => 'INVALID_MONTH',
            'year' => 'INVALID_YEAR',
            'method' => 'INVALID_METHOD',
            'school' => 'INVALID_SCHOOL',
            default => 'VALIDATION_ERROR'
        };

        throw new HttpResponseException(response()->json([
            'success' => false,
            'error' => [
                'code' => $errorCode,
                'message' => $errors->first()
            ]
        ], 422));
    }
}
