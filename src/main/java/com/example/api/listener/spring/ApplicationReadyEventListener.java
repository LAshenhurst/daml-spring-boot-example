package com.example.api.listener.spring;

import com.example.api.common.exceptions.ApiException;
import com.example.api.common.daml.LedgerClient;
import com.example.api.configuration.properties.LedgerProperties;
import com.example.api.helper.LedgerHelper;
import com.example.api.listener.LedgerListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.util.retry.Retry;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApplicationReadyEventListener {
    private final LedgerClient ledgerClient;
    private final LedgerListener ledgerListener;
    private final LedgerProperties ledgerProperties;
    private final ApplicationContext applicationContext;

    @EventListener(ApplicationReadyEvent.class)
    public void connectLedgerClient() {
        LedgerHelper.connect(ledgerClient)
                .retryWhen(Retry
                        .backoff(ledgerProperties.getMaxAttempts(), Duration.ofSeconds(ledgerProperties.getBackoffSeconds()))
                        .onRetryExhaustedThrow(((retryBackoffSpec, retrySignal) -> {
                            AvailabilityChangeEvent.publish(applicationContext, LivenessState.BROKEN);
                            return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to connect to the Ledger");
                        }))
                )
                .doOnSuccess(sub -> {
                    ledgerClient.getPartyManagementClient()
                            .getKnownParties(null)
                            .subscribe(partyDetails -> {
                                // display name is optional, and probably will not be provided with any automatically generated parties
                                String value = StringUtils.isEmpty(partyDetails.getDisplayName()) ? partyDetails.getParty() : partyDetails.getDisplayName();
                                ledgerProperties.getLedgerPartyIdentifierMap().put(partyDetails.getParty(), value);
                            });
                    ledgerListener.readActiveContracts();

                })
                .subscribe();
    }
}
