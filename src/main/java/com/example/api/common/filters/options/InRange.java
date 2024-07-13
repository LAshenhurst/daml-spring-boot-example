package com.example.api.common.filters.options;

import com.example.api.common.filters.SimpleFilterOptions;
import org.springframework.data.relational.core.query.Criteria;

public class InRange<T> implements SimpleFilterOptions<T> {
    public Criteria toCriteria(String columnName, T filter, T filterTo) {
        return Criteria.where(columnName).between(filter, filterTo);
    }
}
