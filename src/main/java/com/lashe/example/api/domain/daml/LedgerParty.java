package com.lashe.example.api.domain.daml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerParty {
    @NotNull
    private String identifier;

    @NotNull
    private String displayName;

    @NotNull
    private boolean isLocal;
}
