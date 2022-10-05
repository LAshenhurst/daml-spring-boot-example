package com.lashe.example.api.common.filters.impl;

import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.common.filters.SimpleColumnFilter;
import com.lashe.example.api.common.filters.SimpleFilterOptions;
import com.gft.example.api.common.filters.options.*;
import com.lashe.example.api.common.filters.options.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.http.HttpStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NumberColumnFilter extends SimpleColumnFilter<Double> {
    private Double filter;
    private Double filterTo;

    public NumberColumnFilter(String type, Double filter, Double filterTo) {
        super.type = type;
        this.filter = filter;
        this.filterTo = filterTo;
    }

    @Override
    public Criteria toCriteria(String columnName) {
        SimpleFilterOptions<Double> simpleFilterOptions = resolveSimpleFilterOptions(columnName);
        return simpleFilterOptions.toCriteria(columnName, filter, filterTo);
    }

    @Override
    public SimpleFilterOptions<Double> resolveSimpleFilterOptions(String columnName) {
        if ("empty".equals(type)) { return new Empty<>(); }
        else if ("equals".equals(type)) { return new Equals<>(); }
        else if ("notEqual".equals(type)) { return new NotEqual<>(); }
        else if ("lessThan".equals(type)) { return new LessThan<>(); }
        else if ("lessThanOrEqual".equals(type)) { return new LessThanOrEqual<>(); }
        else if ("greaterThan".equals(type)) { return new GreaterThan<>(); }
        else if ("greaterThanOrEqual".equals(type)) { return new GreaterThanOrEqual<>(); }
        else if ("inRange".equals(type)) { return new InRange<>(); }
        else {
            throw new ApiException(HttpStatus.BAD_REQUEST, String.format(TYPE_ERROR_MESSAGE, type, columnName));
        }
    }
}
