package com.example.api.common.filters.options;

import com.example.api.common.filters.SimpleFilterOptions;
import org.springframework.data.relational.core.query.Criteria;

public class GreaterThanOrEqual<T> implements SimpleFilterOptions<T> {
    public Criteria toCriteria(String columnName, T filter, T filterTo) {
        return Criteria.where(columnName).greaterThanOrEquals(filter);
    }
}
