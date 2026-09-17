<?php

return [

    /*
    |--------------------------------------------------------------------------
    | AlAdhan Calendar API Configuration
    |--------------------------------------------------------------------------
    |
    | Configuration for interacting with the official AlAdhan Islamic Calendar
    | API endpoints (gToH, hToG, gToHCalendar, hToGCalendar, holidays).
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
        'CALENDAR_CACHE_ENABLED',
        true
    ),

    'cache_ttl' => (int) env(
        'CALENDAR_CACHE_TTL',
        86400 // default 24 hours
    ),

    'cache_ttl_conversion' => (int) env(
        'CALENDAR_CACHE_TTL_CONVERSION',
        86400 * 30 // 30 days for conversions
    ),

    'cache_ttl_month' => (int) env(
        'CALENDAR_CACHE_TTL_MONTH',
        86400 * 30 // 30 days for monthly calendars
    ),

    'cache_ttl_holidays' => (int) env(
        'CALENDAR_CACHE_TTL_HOLIDAYS',
        86400 * 7 // 7 days for holidays
    ),

    'rate_limit' => (int) env(
        'CALENDAR_RATE_LIMIT',
        60
    ),

    'default_timezone' => env(
        'DEFAULT_TIMEZONE',
        'Africa/Algiers'
    ),

    'default_country' => env(
        'DEFAULT_COUNTRY',
        'Algeria'
    ),

    'default_method' => env(
        'DEFAULT_METHOD',
        'default'
    ),

];
