package com.example.api.common.filters;

import org.springframework.data.relational.core.query.Criteria;

public interface SimpleFilterOptions<T> {
    Criteria toCriteria(String columnName, T filter, T filterTo);
}
