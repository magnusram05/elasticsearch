/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */

package org.elasticsearch.xpack.sql.jdbc;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;
import java.util.function.Function;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MILLI_OF_SECOND;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;

/**
 * JDBC specific datetime specific utility methods. Because of lack of visibility, this class borrows code
 * from {@code org.elasticsearch.xpack.sql.util.DateUtils} and {@code org.elasticsearch.xpack.sql.proto.StringUtils}.
 */
final class JdbcDateUtils {

    private JdbcDateUtils() {
    }

    private static final long DAY_IN_MILLIS = 60 * 60 * 24 * 1000L;

    static final DateTimeFormatter ISO_WITH_MILLIS = new DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .append(ISO_LOCAL_DATE)
        .appendLiteral('T')
        .appendValue(HOUR_OF_DAY, 2)
        .appendLiteral(':')
        .appendValue(MINUTE_OF_HOUR, 2)
        .appendLiteral(':')
        .appendValue(SECOND_OF_MINUTE, 2)
        .appendFraction(MILLI_OF_SECOND, 3, 3, true)
        .appendOffsetId()
        .toFormatter(Locale.ROOT);

    private static ZonedDateTime asDateTime(String date) {
        return ISO_WITH_MILLIS.parse(date, ZonedDateTime::from);
    }

    static long asMillisSinceEpoch(String date) {
        return asDateTime(date).toInstant().toEpochMilli();
    }

    static Date asDate(String date) {
        ZonedDateTime zdt = asDateTime(date);
        return new Date(zdt.toLocalDate().atStartOfDay(zdt.getZone()).toInstant().toEpochMilli());
    }

    /**
     * In contrast to {@link JdbcDateUtils#asDate(String)} here we just want to eliminate
     * the date part and just set it to EPOCH (1970-01-1)
     */
    static Time asTime(long millisSinceEpoch) {
        return new Time(utcMillisRemoveDate(millisSinceEpoch));
    }

    /**
     * In contrast to {@link JdbcDateUtils#asDate(String)} here we just want to eliminate
     * the date part and just set it to EPOCH (1970-01-1)
     */
    static Time asTime(String date) {
        return asTime(asMillisSinceEpoch(date));
    }

    static Timestamp asTimestamp(long millisSinceEpoch) {
        return new Timestamp(millisSinceEpoch);
    }

    static Timestamp asTimestamp(String date) {
        return new Timestamp(asMillisSinceEpoch(date));
    }

    /*
     * Handles the value received as parameter, as either String (a ZonedDateTime formatted in ISO 8601 standard with millis) -
     * date fields being returned formatted like this. Or a Long value, in case of Histograms.
     */
    static <R> R asDateTimeField(Object value, Function<String, R> asDateTimeMethod, Function<Long, R> ctor) {
        if (value instanceof String) {
            return asDateTimeMethod.apply((String) value);
        } else {
            return ctor.apply(((Number) value).longValue());
        }
    }

    private static long utcMillisRemoveDate(long l) {
        return l % DAY_IN_MILLIS;
    }
}
