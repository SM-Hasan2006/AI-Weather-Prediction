package com.aiweather.entity;

public enum AlertOrigin {
    /** Typed in by an administrator from an official source. NOT automatically verified. */
    ADMIN_ENTERED_OFFICIAL,
    /** Produced by this application's rule-based screening. NOT an official warning. */
    SYSTEM_GENERATED
}
