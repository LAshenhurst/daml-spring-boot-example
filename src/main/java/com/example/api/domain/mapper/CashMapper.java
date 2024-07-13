package com.example.api.domain.mapper;

import com.example.api.configuration.properties.LedgerProperties;
import com.example.api.domain.asset.AssetState;
import com.example.api.domain.asset.cash.Cash;
import com.example.api.domain.asset.cash.CashRequest;
import com.example.api.domain.entity.CashEntity;
import com.example.api.domain.entity.CashRequestEntity;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Currency;

@Mapper(componentModel = "spring")
public class CashMapper {
    @Autowired
    private LedgerProperties ledgerProperties;

    public Cash toDomain(CashEntity source) {
        return Cash.builder()
                .identifier(source.getIdentifier())
                .custodian(source.getCustodian())
                .owner(source.getOwner())
                .amount(BigDecimal.valueOf(source.getAmount()))
                .currency(Currency.getInstance(source.getCurrency()))
                .contractId(source.getContractId())
                .state(AssetState.valueOf(source.getState()))
                .latestUpdateTimestamp(source.getLatestUpdateTimestamp())
                .build();
    }

    public CashEntity toEntity(main.assets.Cash source, String contractId) {
        return CashEntity.builder()
                .identifier(source.identifier)
                .custodian(ledgerProperties.getLedgerDisplayName(source.custodian))
                .owner(ledgerProperties.getLedgerDisplayName(source.owner))
                .amount(source.amount.doubleValue())
                .currency(source.currency)
                .contractId(contractId)
                .latestUpdateTimestamp(source.latestUpdateTimestamp)
                .state(source.state.name())
                .build();
    }

    public CashRequest toDomain(CashRequestEntity source) {
        return CashRequest.builder()
                .custodian(source.getCustodian())
                .requester(source.getRequester())
                .currency(Currency.getInstance(source.getCurrency()))
                .amount(source.getAmount())
                .contractId(source.getContractId())
                .build();
    }

    public CashRequestEntity toEntity(main.assets.CashRequest source, String contractId) {
        return CashRequestEntity.builder()
                .custodian(ledgerProperties.getLedgerDisplayName(source.custodian))
                .requester(ledgerProperties.getLedgerDisplayName(source.requester))
                .amount(source.amount.doubleValue())
                .currency(source.currency)
                .contractId(contractId)
                .build();
    }
}
