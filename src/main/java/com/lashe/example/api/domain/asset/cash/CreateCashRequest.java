package com.lashe.example.api.domain.asset.cash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Currency;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCashRequest {
    @NotNull
    private Double amount;

    @NotNull
    private Currency currency;
}
