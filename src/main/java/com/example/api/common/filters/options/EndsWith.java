package com.example.api.common.filters.options;

import com.example.api.common.filters.SimpleFilterOptions;
import org.springframework.data.relational.core.query.Criteria;

public class EndsWith implements SimpleFilterOptions<String> {
    public Criteria toCriteria(String columnName, String filter, String filterTo) {
        return Criteria.where(columnName).like("%" + filter);
    }
}
