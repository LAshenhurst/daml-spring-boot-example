package com.example.api.domain.daml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @NotNull
    private String transactionId;

    @NotNull
    private String commandId;

    @NotNull
    private String workflowId;

    @NotNull
    private Long effectiveAt;

    @NotNull
    private List<Event> events;

    @NotNull
    private String offset;
}