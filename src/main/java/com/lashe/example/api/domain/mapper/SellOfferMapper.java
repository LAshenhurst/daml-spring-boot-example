package com.lashe.example.api.domain.mapper;

import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.entity.SellOfferEntity;
import com.lashe.example.api.domain.exchange.SellOffer;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Currency;

@Mapper(componentModel = "spring")
public class SellOfferMapper {
    @Autowired
    private LedgerProperties ledgerProperties;

    public SellOffer toDomain(SellOfferEntity source) {
        return SellOffer.builder()
                .custodian(source.getCustodian())
                .seller(source.getSeller())
                .corporation(source.getCorporation())
                .identifier(source.getIdentifier())
                .price(source.getPrice())
                .amount(source.getAmount())
                .currency(Currency.getInstance(source.getCurrency()))
                .latestUpdateTimestamp(source.getLatestUpdateTimestamp())
                .contractId(source.getContractId())
                .build();
    }

    public SellOfferEntity toEntity(main.exchange.SellOffer source, String contractId) {
        return SellOfferEntity.builder()
                .custodian(ledgerProperties.getLedgerDisplayName(source.custodian))
                .seller(ledgerProperties.getLedgerDisplayName(source.seller))
                .corporation(ledgerProperties.getLedgerDisplayName(source.corp))
                .price(source.pricePerShare)
                .amount(source.amount)
                .identifier(source.id)
                .currency(source.currency)
                .latestUpdateTimestamp(source.latestUpdateTimestamp)
                .contractId(contractId)
                .build();
    }
}
