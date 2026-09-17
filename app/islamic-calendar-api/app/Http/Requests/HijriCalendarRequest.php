<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class HijriCalendarRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'month' => ['required', 'integer', 'between:1,12'],
            'year' => ['required', 'integer', 'between:1300,1600'],
            'country' => ['nullable', 'string', 'max:50'],
            'method' => ['nullable', 'string', 'max:50'],
            'adjustment' => ['nullable', 'integer', 'between:-2,2']
        ];
    }

    protected function failedValidation(Validator $validator)
    {
        $code = 'INVALID_MONTH';
        if ($validator->errors()->has('year')) {
            $code = 'INVALID_YEAR';
        } elseif ($validator->errors()->has('country')) {
            $code = 'INVALID_COUNTRY';
        } elseif ($validator->errors()->has('method')) {
            $code = 'INVALID_METHOD';
        }

        throw new HttpResponseException(response()->json([
            'success' => false,
            'error' => [
                'code' => $code,
                'message' => $validator->errors()->first()
            ]
        ], 422));
    }
}
