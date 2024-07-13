package com.example.api.domain.mapper;

import com.example.api.configuration.properties.LedgerProperties;
import com.example.api.domain.asset.AssetState;
import com.example.api.domain.asset.share.Share;
import com.example.api.domain.asset.share.ShareRequest;
import com.example.api.domain.entity.ShareEntity;
import com.example.api.domain.entity.ShareRequestEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public class ShareMapper {
    @Autowired
    private LedgerProperties ledgerProperties;

    public Share toDomain(ShareEntity source) {
        return Share.builder()
                .identifier(source.getIdentifier())
                .custodian(source.getCustodian())
                .owner(source.getOwner())
                .corporation(source.getCorporation())
                .amount(source.getAmount())
                .contractId(source.getContractId())
                .state(AssetState.valueOf(source.getState()))
                .latestUpdateTimestamp(source.getLatestUpdateTimestamp())
                .build();
    }

    public ShareEntity toEntity(main.assets.Shares source, String contractId) {
        return ShareEntity.builder()
                .identifier(source.identifier)
                .custodian(ledgerProperties.getLedgerDisplayName(source.custodian))
                .owner(ledgerProperties.getLedgerDisplayName(source.owner))
                .corporation(ledgerProperties.getLedgerDisplayName(source.corp))
                .amount(source.amount)
                .contractId(contractId)
                .state(source.state.name())
                .latestUpdateTimestamp(source.latestUpdateTimestamp)
                .build();
    }

    public ShareRequest toDomain(ShareRequestEntity source) {
        return ShareRequest.builder()
                .custodian(source.getCustodian())
                .requester(source.getRequester())
                .amount(source.getAmount())
                .contractId(source.getContractId())
                .build();
    }

    public ShareRequestEntity toEntity(main.assets.ShareRequest source, String contractId) {
        return ShareRequestEntity.builder()
                .custodian(ledgerProperties.getLedgerDisplayName(source.custodian))
                .requester(ledgerProperties.getLedgerDisplayName(source.requester))
                .amount(source.amount.doubleValue())
                .contractId(contractId)
                .build();
    }
}
