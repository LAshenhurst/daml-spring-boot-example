package com.example.api.domain.daml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateLedgerParty {
    @NotNull
    private String identifierHint;

    @NotNull
    private String displayName;
}
