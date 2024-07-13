package com.example.api.domain.daml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Identifier {
    @NotNull
    private String packageId;

    @NotNull
    private String moduleName;

    @NotNull
    private String entityName;
}