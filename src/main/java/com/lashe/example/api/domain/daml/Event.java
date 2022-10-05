package com.lashe.example.api.domain.daml;

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
public class Event {
    @NotNull
    private List<String> witnessParties;

    @NotNull
    private String eventId;

    @NotNull
    private Identifier templateId;

    @NotNull
    private String contractId;
}