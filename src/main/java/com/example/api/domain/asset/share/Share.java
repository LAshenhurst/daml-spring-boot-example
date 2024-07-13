package com.example.api.domain.asset.share;

import com.example.api.domain.asset.AssetState;
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
public class Share {
    @NotNull
    private String custodian;

    @NotNull
    private String owner;

    @NotNull
    private String corporation;

    @NotNull
    private String identifier;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private AssetState state;

    private String contractId;

    private Long latestUpdateTimestamp;
}
