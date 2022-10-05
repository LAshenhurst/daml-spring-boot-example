package com.lashe.example.api.domain.asset.cash;

import com.lashe.example.api.domain.asset.AssetState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Currency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cash {
    @NotNull
    private String identifier;

    @NotNull
    private String custodian;

    @NotNull
    private String owner;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private Currency currency;

    @NotNull
    private AssetState state;

    private String contractId;

    private Long latestUpdateTimestamp;
}
