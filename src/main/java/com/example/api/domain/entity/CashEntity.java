package com.example.api.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import javax.validation.constraints.NotNull;

@Data
@Builder
@Table("CASH")
@AllArgsConstructor
@NoArgsConstructor
public class CashEntity {
    @Id
    @NotNull
    private Long id;

    @NotNull
    private String identifier;

    @NotNull
    private String custodian;

    @NotNull
    private String owner;

    @NotNull
    private Double amount;

    @NotNull
    private String currency;

    @NotNull
    private String contractId;

    @NotNull
    private Long latestUpdateTimestamp;

    @NotNull
    private String state;

    @Version
    private Long entityVersion;
}
