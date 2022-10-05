package com.lashe.example.api.domain.rest.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageModel {
    private Integer page = 0;

    private Integer size = 100;
}
