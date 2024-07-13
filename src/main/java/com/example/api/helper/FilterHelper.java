package com.example.api.helper;

import com.example.api.common.exceptions.ApiException;
import com.example.api.domain.rest.request.FilterRequest;
import com.example.api.domain.rest.request.PageModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class FilterHelper {
    private static final PageModel DEFAULT_PAGE_MODEL = new PageModel(0, 30);

    public static Criteria newCriteria(FilterRequest filterRequest, Class<?> filterSubject) {
        List<Criteria> criteriaList = new ArrayList<>();
        List<String> fieldNames = Arrays.stream(filterSubject.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        if (filterRequest.getFilterModel() != null) {
            filterRequest.getFilterModel().forEach((key, value) -> {
                if (!fieldNames.contains(key)) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, String.format("No such column: '%s'", key));
                }
                criteriaList.add(value.toCriteria(key));
            });
        }

        return criteriaList.isEmpty() ? Criteria.empty() : Criteria.from(criteriaList);
    }

    public static PageRequest newPageRequest(FilterRequest filterRequest, Class<?> filterSubject) {
        PageModel pageModel = filterRequest.getPageModel() == null ? DEFAULT_PAGE_MODEL : filterRequest.getPageModel();
        List<Sort.Order> orderList = new ArrayList<>();
        List<String> fieldNames = Arrays.stream(filterSubject.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toList());

        if (filterRequest.getSortModel() != null) {
            filterRequest.getSortModel().forEach(sortModel -> {
                if (!fieldNames.contains(sortModel.getColumnName())) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, String.format("No such column: '%s'", sortModel.getColumnName()));
                }

                if (sortModel.getDirection().isAscending()) {
                    orderList.add(Sort.Order.asc(sortModel.getColumnName()));
                } else if (sortModel.getDirection().isDescending()) {
                    orderList.add(Sort.Order.desc(sortModel.getColumnName()));
                }
            });
        }

        return PageRequest.of(pageModel.getPage(), pageModel.getSize(), Sort.by(orderList));
    }

    private FilterHelper() { throw new IllegalStateException("Utility class."); }
}