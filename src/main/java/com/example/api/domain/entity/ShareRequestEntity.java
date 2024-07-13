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
@NoArgsConstructor
@AllArgsConstructor
@Table("SHARE_REQUESTS")
public class ShareRequestEntity {
    @Id
    @NotNull
    private Long id;

    @NotNull
    private String custodian;

    @NotNull
    private String requester;

    @NotNull
    private Double amount;

    @NotNull
    private String contractId;

    @Version
    private Long entityVersion;
}
