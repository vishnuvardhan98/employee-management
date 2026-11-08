package com.peopledesk.employee;

import com.peopledesk.common.BadRequestException;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Maps the API's public sort keys to entity properties. Only whitelisted keys are accepted, so
 * clients cannot sort by arbitrary (or non-existent) columns.
 */
public final class EmployeeSort {

    public static final int MAX_PAGE_SIZE = 100;

    private static final Map<String, String> PROPERTIES = Map.of(
            "name", "name",
            "email", "email",
            "title", "title",
            "department", "department.name",
            "status", "status",
            "joined", "joinedOn");

    private EmployeeSort() {
    }

    public static Sort of(String key, String direction) {
        String property = PROPERTIES.get(key == null ? "name" : key);
        if (property == null) {
            throw new BadRequestException("Unsupported sort field '" + key + "'.");
        }
        Sort.Direction dir = "desc".equals(direction == null ? "asc" : direction.toLowerCase(Locale.ROOT))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        // Secondary sort on id keeps page boundaries stable when primary values tie.
        return Sort.by(dir, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    public static Pageable pageable(int page, int size, String key, String direction) {
        if (page < 0) {
            throw new BadRequestException("Page must be zero or greater.");
        }
        int boundedSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        return PageRequest.of(page, boundedSize, of(key, direction));
    }
}
