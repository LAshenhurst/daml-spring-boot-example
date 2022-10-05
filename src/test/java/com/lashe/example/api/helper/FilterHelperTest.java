package com.lashe.example.api.helper;

import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.domain.asset.cash.Cash;
import com.lashe.example.api.domain.rest.request.FilterRequest;
import com.lashe.example.api.domain.rest.request.SortModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.List;

@SpringBootTest
@ExtendWith(SpringExtension.class)
class FilterHelperTest {
    @Test
    void Given_NoFilter_When_CreatingCriteria_Then_EmptyCriteriaReturned() {
        Assertions.assertTrue(FilterHelper.newCriteria(FilterRequest.builder().build(), Cash.class).isEmpty());
    }

    @Test
    void Given_BadFieldName_When_CreatingCriteria_Then_ErrorReturned() {
        FilterRequest filterRequest = FilterRequest.builder()
                .filterModel(Collections.singletonMap("badField", null))
                .build();
        Assertions.assertThrows(ApiException.class, () -> FilterHelper.newCriteria(filterRequest, Cash.class));
    }

    @Test
    void Given_EmptyFilterRequest_When_CreatingPageable_Then_DefaultPageableReturned() {
        Pageable defaultPageable = PageRequest.of(0, 30, Sort.unsorted());
        Assertions.assertEquals(FilterHelper.newPageRequest(FilterRequest.builder().build(), Cash.class), defaultPageable);
    }

    @Test
    void Given_BadFieldName_When_CreatingPageable_Then_ErrorReturned() {
        SortModel sortModel = SortModel.builder()
                .columnName("badFieldName")
                .direction(Sort.Direction.DESC)
                .build();

        FilterRequest filterRequest = FilterRequest.builder()
                .sortModel(Collections.singletonList(sortModel))
                .build();

        Assertions.assertThrows(ApiException.class, () -> FilterHelper.newPageRequest(filterRequest, Cash.class));
    }

    @Test
    void Given_MultipleSortFields_When_CreatingPageable_Then_MultipleSortsReturned() {
        SortModel currencySort = SortModel.builder().columnName("currency").direction(Sort.Direction.DESC).build();
        SortModel amountSort = SortModel.builder().columnName("amount").direction(Sort.Direction.DESC).build();

        FilterRequest filterRequest = FilterRequest.builder()
                .sortModel(List.of(currencySort, amountSort))
                .build();

        Pageable pageable = FilterHelper.newPageRequest(filterRequest, Cash.class);
        Assertions.assertEquals(2, pageable.getSort().get().count());
    }
}
