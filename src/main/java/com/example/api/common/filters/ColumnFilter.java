package com.example.api.common.filters;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.example.api.common.filters.impl.DateColumnFilter;
import com.example.api.common.filters.impl.NumberColumnFilter;
import com.example.api.common.filters.impl.SetColumnFilter;
import com.example.api.common.filters.impl.TextColumnFilter;
import lombok.Data;
import org.springframework.data.relational.core.query.Criteria;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "filterType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TextColumnFilter.class, name = "text"),
        @JsonSubTypes.Type(value = SetColumnFilter.class, name = "set"),
        @JsonSubTypes.Type(value = NumberColumnFilter.class, name = "number"),
        @JsonSubTypes.Type(value = DateColumnFilter.class, name = "date")
})
public abstract class ColumnFilter {
    protected String filterType;
    protected String value;

    public abstract Criteria toCriteria(String columnName);
}
