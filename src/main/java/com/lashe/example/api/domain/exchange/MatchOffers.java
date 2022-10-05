package com.lashe.example.api.domain.exchange;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchOffers {
    @NotNull
    private String buyOfferIdentifier;

    @NotNull
    private String sellOfferIdentifier;
}
