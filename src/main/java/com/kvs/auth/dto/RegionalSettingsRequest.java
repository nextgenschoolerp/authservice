package com.kvs.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Regional preferences of a tenant. The allowed values are the option lists of the UI
 * (frontend/src/regional.js: TIME_ZONES, CURRENCIES, DATE_FORMATS, LANGUAGES); keep both in step.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegionalSettingsRequest {

    public static final String TIME_ZONES = "Asia/Kolkata|Asia/Kathmandu|Asia/Dhaka|Asia/Colombo|Asia/Dubai|Asia/Riyadh|Asia/Qatar"
            + "|Asia/Singapore|Asia/Kuala_Lumpur|Asia/Jakarta|Asia/Tokyo|Australia/Sydney|Pacific/Auckland|Africa/Nairobi"
            + "|Africa/Lagos|Africa/Johannesburg|Europe/London|Europe/Paris|America/New_York|America/Chicago"
            + "|America/Los_Angeles|America/Toronto|UTC";
    public static final String CURRENCIES = "INR|USD|GBP|EUR|AED|SAR|QAR|SGD|MYR|NPR|BDT|LKR|AUD|NZD|CAD|KES|NGN|ZAR|JPY";
    public static final String DATE_FORMATS = "DD MMM YYYY|D MMMM YYYY|DD/MM/YYYY|DD-MM-YYYY|DD\\.MM\\.YYYY|MM/DD/YYYY"
            + "|MMM D, YYYY|YYYY-MM-DD";
    public static final String LANGUAGES = "en-IN|en-GB|en-US|en-AU|hi-IN|mr-IN|bn-IN|ta-IN|te-IN|gu-IN|kn-IN|ar-AE|fr-FR";

    @NotBlank(message = "Time zone is required")
    @Pattern(regexp = TIME_ZONES, message = "Time zone is not one of the supported IANA zones")
    private String timeZone;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = CURRENCIES, message = "Currency is not one of the supported ISO 4217 codes")
    private String currency;

    @NotBlank(message = "Date format is required")
    @Pattern(regexp = DATE_FORMATS, message = "Date format is not one of the supported patterns")
    private String dateFormat;

    @NotBlank(message = "Language is required")
    @Pattern(regexp = LANGUAGES, message = "Language is not one of the supported locales")
    private String language;
}
