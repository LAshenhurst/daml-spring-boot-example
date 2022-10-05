package com.lashe.example.api.domain.exchange;

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
public class BuyOffer {
    @NotNull
    private String custodian;

    @NotNull
    private String buyer;

    @NotNull
    private String corporation;

    @NotNull
    private String identifier;

    @NotNull
    private BigDecimal price;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private Currency currency;

    @NotNull
    private Long latestUpdateTimestamp;

    private String contractId;
}
