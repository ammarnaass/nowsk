<?php

return [

    /*
    |--------------------------------------------------------------------------
    | AlAdhan API Configuration
    |--------------------------------------------------------------------------
    |
    | Base URL, timeout and cache configuration for calling the upstream
    | AlAdhan Prayer Times API service.
    |
    */

    'base_url' => env(
        'ALADHAN_BASE_URL',
        'https://api.aladhan.com/v1'
    ),

    'timeout' => (int) env(
        'ALADHAN_TIMEOUT',
        10
    ),

    'cache_enabled' => (bool) env(
        'PRAYER_CACHE_ENABLED',
        true
    ),

    'cache_ttl' => (int) env(
        'PRAYER_CACHE_TTL',
        86400
    ),

    'rate_limit' => (int) env(
        'PRAYER_RATE_LIMIT',
        60
    ),

    'default_method' => (int) env(
        'PRAYER_DEFAULT_METHOD',
        19 // Algeria method default (can be overridden per request)
    ),

    'default_school' => (int) env(
        'PRAYER_DEFAULT_SCHOOL',
        0 // 0 = Shafi/Hanbali/Maliki, 1 = Hanafi
    ),

];
