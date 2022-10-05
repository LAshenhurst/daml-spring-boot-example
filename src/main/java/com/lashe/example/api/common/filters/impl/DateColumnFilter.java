package com.lashe.example.api.common.filters.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
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

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateColumnFilter extends SimpleColumnFilter<LocalDate> {
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate dateFrom;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    public LocalDate dateTo;

    public DateColumnFilter(String type, LocalDate dateFrom, LocalDate dateTo) {
        super.type = type;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
    }

    @Override
    public Criteria toCriteria(String columnName) {
        SimpleFilterOptions<LocalDate> simpleFilterOptions = resolveSimpleFilterOptions(columnName);
        return simpleFilterOptions.toCriteria(columnName, dateFrom, dateTo);
    }

    @Override
    public SimpleFilterOptions<LocalDate> resolveSimpleFilterOptions(String columnName) {
        if ("empty".equals(type)) { return new Empty<>(); }
        else if ("equals".equals(type)) { return new Equals<>(); }
        else if ("notEqual".equals(type)) { return new NotEqual<>(); }
        else if ("lessThan".equals(type)) { return new LessThan<>(); }
        else if ("greaterThan".equals(type)) { return new GreaterThan<>(); }
        else if ("inRange".equals(type)) { return new InRange<>(); }
        else {
            throw new ApiException(HttpStatus.BAD_REQUEST, String.format(TYPE_ERROR_MESSAGE, type, columnName));
        }
    }
}
