<?php

namespace App\Http\Requests;

use Illuminate\Contracts\Validation\Validator;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Http\Exceptions\HttpResponseException;

class HolidaysRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'year' => ['required', 'integer', 'between:1300,1600'],
            'country' => ['nullable', 'string', 'max:50'],
            'method' => ['nullable', 'string', 'max:50']
        ];
    }

    protected function failedValidation(Validator $validator)
    {
        $code = 'INVALID_YEAR';
        if ($validator->errors()->has('country')) {
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
