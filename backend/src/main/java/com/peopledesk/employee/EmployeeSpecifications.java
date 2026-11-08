package com.peopledesk.employee;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Builds type-safe Criteria API predicates. All user input is bound as parameters, so search text
 * can never alter the SQL statement (SQL injection prevention).
 */
public final class EmployeeSpecifications {

    /** '!' instead of '\' because MySQL treats backslash in string literals as an escape itself. */
    private static final char LIKE_ESCAPE = '!';

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> matching(EmployeeFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(filter.search())) {
                String pattern = "%" + escapeLike(filter.search().trim().toLowerCase(Locale.ROOT)) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("email")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE)));
            }
            if (filter.departmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("id"), filter.departmentId()));
            }
            if (filter.status() != null) {
                predicates.add(cb.equal(root.get("status"), filter.status()));
            }
            if (filter.joinedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("joinedOn"), filter.joinedFrom()));
            }
            if (filter.joinedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("joinedOn"), filter.joinedTo()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    static String escapeLike(String value) {
        return value
                .replace(String.valueOf(LIKE_ESCAPE), LIKE_ESCAPE + "" + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }
}
