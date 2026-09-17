<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class GregorianToHijriRequest extends FormRequest
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
                'regex:/^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$/',
                'date_format:Y-m-d'
            ],
            'country' => ['nullable', 'string', 'max:50'],
            'method' => ['nullable', 'string', 'max:50'],
            'adjustment' => ['nullable', 'integer', 'between:-2,2']
        ];
    }

    protected function failedValidation(Validator $validator)
    {
        $firstError = $validator->errors()->first();
        $code = 'INVALID_GREGORIAN_DATE';

        if ($validator->errors()->has('country')) {
            $code = 'INVALID_COUNTRY';
        } elseif ($validator->errors()->has('method')) {
            $code = 'INVALID_METHOD';
        }

        throw new HttpResponseException(response()->json([
            'success' => false,
            'error' => [
                'code' => $code,
                'message' => $firstError
            ]
        ], 422));
    }
}
