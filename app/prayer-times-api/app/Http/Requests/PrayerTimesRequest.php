<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class PrayerTimesRequest extends FormRequest
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
            'date' => ['nullable', 'date_format:Y-m-d'],
            'method' => ['nullable', 'integer', 'min:0', 'max:50'],
            'school' => ['nullable', 'integer', 'in:0,1'],
            'tune' => ['nullable', 'regex:/^(-?\d+,){8}-?\d+$/'], // e.g. 0,2,0,0,1,0,0,0,0
        ];
    }

    public function messages(): array
    {
        return [
            'latitude.required' => 'Latitude is required.',
            'latitude.between' => 'Latitude must be between -90 and 90.',
            'longitude.required' => 'Longitude is required.',
            'longitude.between' => 'Longitude must be between -180 and 180.',
            'date.date_format' => 'Date must match format YYYY-MM-DD.',
            'method.integer' => 'Calculation method must be an integer.',
            'school.in' => 'School must be 0 (Shafi) or 1 (Hanafi).',
            'tune.regex' => 'Tune must be comma-separated list of 9 integers (e.g. 0,2,0,0,1,0,0,0,0).'
        ];
    }

    protected function failedValidation(Validator $validator)
    {
        $errors = $validator->errors();
        $firstKey = $errors->keys()[0] ?? 'UNKNOWN';

        $errorCode = match ($firstKey) {
            'latitude' => 'INVALID_LATITUDE',
            'longitude' => 'INVALID_LONGITUDE',
            'date' => 'INVALID_DATE',
            'method' => 'INVALID_METHOD',
            'school' => 'INVALID_SCHOOL',
            'tune' => 'INVALID_TUNE',
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
