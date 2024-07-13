package com.example.api.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
@Table("SHARES")
@AllArgsConstructor
@NoArgsConstructor
public class ShareEntity {
    @Id
    @NotNull
    private Long id;

    @NotNull
    private String identifier;

    @NotNull
    private String custodian;

    @NotNull
    private String corporation;

    @NotNull
    private String owner;

    @NotNull
    private BigDecimal amount;

    @NotNull
    private String contractId;

    @NotNull
    private String state;

    @NotNull
    private Long latestUpdateTimestamp;

    @Version
    private Long entityVersion;
}
