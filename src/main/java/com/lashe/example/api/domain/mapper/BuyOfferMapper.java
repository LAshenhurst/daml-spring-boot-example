package com.lashe.example.api.domain.mapper;

import com.lashe.example.api.configuration.properties.LedgerProperties;
import com.lashe.example.api.domain.entity.BuyOfferEntity;
import com.lashe.example.api.domain.exchange.BuyOffer;
import org.mapstruct.Mapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Currency;

@Mapper(componentModel = "spring")
public class BuyOfferMapper {
    @Autowired
    private LedgerProperties ledgerProperties;

    public BuyOffer toDomain(BuyOfferEntity source) {
        return BuyOffer.builder()
                .custodian(source.getCustodian())
                .buyer(source.getBuyer())
                .corporation(source.getCorporation())
                .identifier(source.getIdentifier())
                .amount(source.getAmount())
                .price(source.getPrice())
                .currency(Currency.getInstance(source.getCurrency()))
                .latestUpdateTimestamp(source.getLatestUpdateTimestamp())
                .contractId(source.getContractId())
                .build();
    }

    public BuyOfferEntity toEntity(main.exchange.BuyOffer source, String contractId) {
        return BuyOfferEntity.builder()
                .custodian(ledgerProperties.getLedgerDisplayName(source.custodian))
                .buyer(ledgerProperties.getLedgerDisplayName(source.buyer))
                .corporation(ledgerProperties.getLedgerDisplayName(source.corp))
                .identifier(source.id)
                .price(source.pricePerShare)
                .amount(source.amount)
                .currency(source.currency)
                .latestUpdateTimestamp(source.latestUpdateTimestamp)
                .contractId(contractId)
                .build();
    }
}
