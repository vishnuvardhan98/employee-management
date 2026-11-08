package com.peopledesk.report;

/** One row of a COUNT ... GROUP BY aggregate (e.g. status → count, department → count). */
public record CategoryCount(String key, Long count) {
}
