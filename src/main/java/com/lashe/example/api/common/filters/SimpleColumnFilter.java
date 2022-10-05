package com.lashe.example.api.common.filters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class SimpleColumnFilter<T> extends ColumnFilter {
    protected static final String TYPE_ERROR_MESSAGE = "Filter type %s not supported for column '%s'";
    protected String type;

    public abstract SimpleFilterOptions<T> resolveSimpleFilterOptions(String columnName);
}
