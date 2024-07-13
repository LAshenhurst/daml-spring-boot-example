package com.example.api.listener.spring;

import com.example.api.common.exceptions.ApiException;
import com.example.api.common.daml.LedgerClient;
import com.example.api.listener.LedgerListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContextClosedEventListener {
    private final LedgerClient ledgerClient;
    private final LedgerListener ledgerListener;

    @EventListener(ContextClosedEvent.class)
    public void closeLedgerConnection() {
        ledgerClient.isConnected()
                .subscribe(connected -> {
                   try {
                       ledgerListener.stopListening();
                       log.info("Closing Ledger connection.");
                       ledgerClient.close();
                   } catch (Exception ex) {
                       log.error("An error occurred when trying to close the Ledger connection!", ex);
                       throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred when trying to close the Ledger connection, please see logs for details");
                   }
                });
    }
}
