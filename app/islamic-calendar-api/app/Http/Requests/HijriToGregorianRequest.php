<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class HijriToGregorianRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'date' => [
                'required',
                'string',
                'regex:/^(0[1-9]|[12]\d|30)-(0[1-9]|1[0-2])-\d{4}$/'
            ],
            'country' => ['nullable', 'string', 'max:50'],
            'method' => ['nullable', 'string', 'max:50'],
            'adjustment' => ['nullable', 'integer', 'between:-2,2']
        ];
    }

    protected function failedValidation(Validator $validator)
    {
        $firstError = $validator->errors()->first();
        $code = 'INVALID_HIJRI_DATE';

        if ($validator->errors()->has('country')) {
            $code = 'INVALID_COUNTRY';
        } elseif ($validator->errors()->has('method')) {
            $code = 'INVALID_METHOD';
        }

        throw new HttpResponseException(response()->json([
            'success' => false,
            'error' => [
                'code' => $code,
                'message' => $firstError ?: 'Invalid Hijri date. Expected format DD-MM-YYYY.'
            ]
        ], 422));
    }
}
