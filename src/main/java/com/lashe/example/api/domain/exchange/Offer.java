package com.lashe.example.api.domain.exchange;

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
public class Offer {
    @NotNull
    private double pricePerShare;

    @NotNull
    private String corp;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String currency;
}
