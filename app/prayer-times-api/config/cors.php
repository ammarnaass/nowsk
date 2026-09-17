<?php

return [

    /*
    |--------------------------------------------------------------------------
    | Cross-Origin Resource Sharing (CORS) Configuration
    |--------------------------------------------------------------------------
    |
    | Strictly configured according to PRD Section 29 to allow client apps
    | while preventing open '*' wildcards.
    |
    */

    'paths' => ['api/*', 'docs', 'docs/*'],

    'allowed_methods' => ['GET', 'OPTIONS'],

    'allowed_origins' => [
        env('CORS_ALLOWED_ORIGIN', 'http://localhost:3000'),
        'http://localhost:8080',
        'http://127.0.0.1:8000',
    ],

    'allowed_origins_patterns' => [],

    'allowed_headers' => ['Content-Type', 'X-Requested-With', 'Authorization', 'Accept'],

    'exposed_headers' => [],

    'max_age' => 86400,

    'supports_credentials' => false,

];
