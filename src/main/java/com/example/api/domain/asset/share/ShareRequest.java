package com.example.api.domain.asset.share;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShareRequest {
    @NotNull
    private String custodian;

    @NotNull
    private String requester;

    @NotNull
    private Double amount;

    @NotNull
    private String contractId;
}
