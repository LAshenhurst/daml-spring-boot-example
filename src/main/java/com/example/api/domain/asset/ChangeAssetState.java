package com.example.api.domain.asset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeAssetState {
    @NotNull
    private String identifier;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private AssetState newState;
}
