package com.example.api.common.filters.impl;

import com.example.api.common.filters.SimpleColumnFilter;
import com.example.api.common.filters.SimpleFilterOptions;
import com.example.api.common.filters.options.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.query.Criteria;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TextColumnFilter extends SimpleColumnFilter<String> {
    private String filter;

    public TextColumnFilter(String type, String filter) {
        super.type = type;
        this.filter = filter;
    }

    @Override
    public Criteria toCriteria(String columnName) {
        SimpleFilterOptions<String> simpleFilterOptions = resolveSimpleFilterOptions(columnName);
        return simpleFilterOptions.toCriteria(columnName, filter, null);
    }

    @Override
    public SimpleFilterOptions<String> resolveSimpleFilterOptions(String columnName) {
        if ("empty".equals(type)) { return new Empty<>(); }
        else if ("equals".equals(type)) { return new Equals<>(); }
        else if ("notEqual".equals(type)) { return new NotEqual<>(); }
        else if ("contains".equals(type)) { return new Contains(); }
        else if ("notContains".equals(type)) { return new NotContains(); }
        else if ("startsWith".equals(type)) { return new StartsWith(); }
        else if ("endsWith".equals(type)) { return new EndsWith(); }

        return new Empty<>();
    }
}
