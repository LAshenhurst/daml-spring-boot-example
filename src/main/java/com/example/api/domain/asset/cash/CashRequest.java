package com.example.api.domain.asset.cash;

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
public class CashRequest {
    @NotNull
    private String custodian;

    @NotNull
    private String requester;

    @NotNull
    private Currency currency;

    @NotNull
    private Double amount;

    @NotNull
    private String contractId;
}
