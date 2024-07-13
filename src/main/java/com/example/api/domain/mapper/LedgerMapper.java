package com.example.api.domain.mapper;

import com.daml.ledger.api.v1.admin.PackageManagementServiceOuterClass;
import com.daml.ledger.api.v1.admin.PartyManagementServiceOuterClass;
import com.example.api.common.exceptions.ApiException;
import com.example.api.domain.asset.AssetState;
import com.google.protobuf.Timestamp;
import com.example.api.domain.daml.*;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.stream.Collectors;

@Slf4j
@Mapper(componentModel = "spring")
public class LedgerMapper {
    public PackageDetails toDomain(PackageManagementServiceOuterClass.PackageDetails source) {
        return PackageDetails.builder()
                .identifier(source.getPackageId())
                .knownSince(toLocalDateTime(source.getKnownSince()))
                .sourceDescription(source.getSourceDescription())
                .build();
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return Instant
                .ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public LedgerParty toDomain(PartyManagementServiceOuterClass.PartyDetails source) {
        return LedgerParty.builder()
                .displayName(source.getDisplayName())
                .identifier(source.getParty())
                .build();
    }

    public main.utils.AssetState toDaml(AssetState assetState) {
        switch (assetState) {
            case AVAI: return main.utils.AssetState.AVAI;
            case LOCKED: return main.utils.AssetState.LOCKED;
            case REVOKED: return main.utils.AssetState.REVOKED;
            default:
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unknown AssetState found!");
        }
    }

    public Transaction toDomain(com.daml.ledger.javaapi.data.Transaction source) {
        return Transaction.builder()
                .commandId(source.getCommandId())
                .effectiveAt(source.getEffectiveAt().getEpochSecond())
                .offset(source.getOffset())
                .transactionId(source.getTransactionId())
                .workflowId(source.getWorkflowId())
                .events(
                        source.getEvents()
                                .stream()
                                .map(this::toDomain)
                                .collect(Collectors.toList())
                )
                .build();
    }

    private Event toDomain(com.daml.ledger.javaapi.data.Event source) {
        return Event.builder()
                .contractId(source.getContractId())
                .eventId(source.getEventId())
                .witnessParties(source.getWitnessParties())
                .templateId(toDomain(source.getTemplateId()))
                .build();
    }

    private Identifier toDomain(com.daml.ledger.javaapi.data.Identifier source) {
        return Identifier.builder()
                .entityName(source.getEntityName())
                .moduleName(source.getModuleName())
                .packageId(source.getPackageId())
                .build();
    }
}
