<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class AddressPrayerTimesRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'address' => ['required', 'string', 'min:2', 'max:255'],
            'date' => ['nullable', 'date_format:Y-m-d'],
            'method' => ['nullable', 'integer', 'min:0', 'max:50'],
            'school' => ['nullable', 'integer', 'in:0,1'],
            'tune' => ['nullable', 'regex:/^(-?\d+,){8}-?\d+$/'],
        ];
    }

    public function messages(): array
    {
        return [
            'address.required' => 'Address is required.',
            'date.date_format' => 'Date must match format YYYY-MM-DD.',
            'method.integer' => 'Calculation method must be an integer.',
            'school.in' => 'School must be 0 (Shafi) or 1 (Hanafi).',
        ];
    }

    protected function failedValidation(Validator $validator)
    {
        $errors = $validator->errors();
        $firstKey = $errors->keys()[0] ?? 'UNKNOWN';

        $errorCode = match ($firstKey) {
            'address' => 'INVALID_ADDRESS',
            'date' => 'INVALID_DATE',
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
