package com.kvs.auth.entity;

import com.kvs.core.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Per-tenant regional preferences (Settings → General → Regional preferences): time zone, currency,
 * date format and language used by the UI to format every date, time, number and amount.
 * One row per tenant; a tenant without a row uses the defaults.
 */
@Entity
@Table(name = "tenant_regional_settings")
@Getter
@Setter
public class TenantRegionalSettings extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", unique = true, nullable = false, length = 100)
    private String tenantId;

    /** IANA time zone, e.g. Asia/Kolkata */
    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    /** ISO 4217 currency code, e.g. INR */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    /** Display pattern, e.g. DD MMM YYYY */
    @Column(name = "date_format", nullable = false, length = 20)
    private String dateFormat;

    /** BCP 47 locale for dates and numbers, e.g. en-IN */
    @Column(name = "language", nullable = false, length = 10)
    private String language;
}
