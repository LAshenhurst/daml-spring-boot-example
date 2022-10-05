package com.lashe.example.api.common.filters.impl;

import com.lashe.example.api.common.filters.ColumnFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.query.Criteria;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SetColumnFilter extends ColumnFilter {
    private List<String> values;

    @Override
    public Criteria toCriteria(String columnName) { return Criteria.where(columnName).in(values); }
}
