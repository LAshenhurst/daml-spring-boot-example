package com.lashe.example.api.domain.rest.request;

import com.lashe.example.api.common.filters.ColumnFilter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterRequest {
    private PageModel pageModel = new PageModel();

    private List<SortModel> sortModel;

    private Map<String, ColumnFilter> filterModel;
}
