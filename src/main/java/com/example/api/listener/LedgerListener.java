package com.example.api.listener;

import com.daml.ledger.javaapi.data.*;
import com.example.api.common.daml.LedgerClientAdapter;
import com.example.api.common.exceptions.ApiException;
import com.example.api.configuration.properties.LedgerProperties;
import com.example.api.service.CashService;
import com.example.api.service.ExchangeService;
import com.example.api.service.ShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import main.assets.Cash;
import main.assets.CashRequest;
import main.assets.ShareRequest;
import main.exchange.BuyOffer;
import main.exchange.SellOffer;
import main.assets.Shares;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.LivenessState;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Slf4j
public class LedgerListener {

	private Disposable listener;
	private final LedgerClientAdapter ledgerClientAdapter;
	private final CashService cashService;
	private final ShareService shareService;
	private final ExchangeService exchangeService;
	private final ApplicationContext applicationContext;
	private final LedgerProperties ledgerProperties;
	private final AtomicReference<String> offset = new AtomicReference<>();

	private static final Set<Identifier> FILTER_IDENTIFIERS = Set.of(
			Cash.TEMPLATE_ID,
			CashRequest.TEMPLATE_ID,
			Shares.TEMPLATE_ID,
			ShareRequest.TEMPLATE_ID,
			BuyOffer.TEMPLATE_ID,
			SellOffer.TEMPLATE_ID
	);

	public void readActiveContracts() {
		listener = ledgerClientAdapter.read(ledgerProperties.getCustodianPartyId(), FILTER_IDENTIFIERS)
				.doOnSubscribe(sub -> log.info("Reading the Active Contract Set"))
				.doFinally(sub -> {
					log.info("Finished reading the Active Contract Set");
					startTransactionListener();
				})
				.subscribe(getActiveContractsResponse -> {
					if (getActiveContractsResponse != null) {
						log.info("getActiveContractResponse found: {}", getActiveContractsResponse);
						getActiveContractsResponse.getCreatedEvents().forEach(this::processEvent);
						if (getActiveContractsResponse.getOffset().isPresent()) {
							log.info("offset found: {}", getActiveContractsResponse.getOffset().get());
							offset.set(getActiveContractsResponse.getOffset().get());
						}
					}
				});
	}

	private void processEvent(Event event) {
		log.debug("Found event: {}", event);
		if (event instanceof CreatedEvent) { processCreatedEvent((CreatedEvent) event); }
		else if (event instanceof ArchivedEvent) { processArchivedEvent((ArchivedEvent) event); }
		else {
			log.warn("Unknown Ledger Event type found!! Type: {}", event.getClass().getSimpleName());
		}
	}

	private void processCreatedEvent(CreatedEvent event) {
		if (Cash.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Created Cash found: {}", Cash.Contract.fromCreatedEvent(event).data);
			cashService.insertOrUpdateCash(Cash.Contract.fromCreatedEvent(event).data, event.getContractId()).subscribe();
		} else if (CashRequest.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Created CashRequest found: {}", CashRequest.Contract.fromCreatedEvent(event).data);
			cashService.insertCashRequest(CashRequest.Contract.fromCreatedEvent(event).data, event.getContractId()).subscribe();
		} else if (Shares.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Created Shares found: {}", Shares.Contract.fromCreatedEvent(event).data);
			shareService.insertOrUpdateShares(Shares.Contract.fromCreatedEvent(event).data, event.getContractId()).subscribe();
		} else if (ShareRequest.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Created ShareRequest found; {}", ShareRequest.Contract.fromCreatedEvent(event).data);
			shareService.insertShareRequest(ShareRequest.Contract.fromCreatedEvent(event).data, event.getContractId()).subscribe();
		} else if (BuyOffer.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Created BuyOffer found: {}", BuyOffer.Contract.fromCreatedEvent(event).data);
			exchangeService.insertOrUpdateBuyOffer(BuyOffer.Contract.fromCreatedEvent(event).data, event.getContractId()).subscribe();
		} else if (SellOffer.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Created SellOffer found: {}", SellOffer.Contract.fromCreatedEvent(event).data);
			exchangeService.insertOrUpdateSellOffer(SellOffer.Contract.fromCreatedEvent(event).data, event.getContractId()).subscribe();
		}
	}

	private void processArchivedEvent(ArchivedEvent event) {
		if (Cash.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Archived Cash found with contractId: {}", event.getContractId());
			cashService.deleteCash(event.getContractId()).subscribe();
		} else if (CashRequest.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Archived CashRequest found with contractId: {}", event.getContractId());
			cashService.deleteCashRequest(event.getContractId()).subscribe();
		} else if (Shares.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Archived Shares found with contractId: {}", event.getContractId());
			shareService.deleteShares(event.getContractId()).subscribe();
		} else if (ShareRequest.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Archived ShareRequest found with contractId: {}", event.getContractId());
			shareService.deleteShareRequest(event.getContractId()).subscribe();
		} else if (BuyOffer.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Archived BuyOffer found with contractId: {}", event.getContractId());
			exchangeService.deleteBuyOffer(event.getContractId()).subscribe();
		} else if (SellOffer.TEMPLATE_ID.equals(event.getTemplateId())) {
			log.debug("Archived SellOffer found with contractId: {}", event.getContractId());
			exchangeService.deleteSellOffer(event.getContractId()).subscribe();
		}
	}

	private void startTransactionListener() {
		LedgerOffset startOffset = offset.get() != null ? new LedgerOffset.Absolute(offset.get()) : LedgerOffset.LedgerBegin.getInstance();
		listener = ledgerClientAdapter.listen(startOffset, ledgerProperties.getCustodianPartyId(), FILTER_IDENTIFIERS)
				.doOnSubscribe(sub -> log.info("Start listening to transactions events from ledger"))
				.retryWhen(Retry
						.backoff(ledgerProperties.getMaxAttempts(), Duration.ofSeconds(ledgerProperties.getBackoffSeconds()))
						.onRetryExhaustedThrow(((retryBackoffSpec, retrySignal) -> {
							AvailabilityChangeEvent.publish(applicationContext, LivenessState.BROKEN);
							throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to connect to the Ledger");
						}))
				)
				.subscribe(transaction -> {
					if (transaction != null) {
						transaction.getEvents().forEach(this::processEvent);
					}
				});
	}

	public Boolean isListening() { return listener != null && !listener.isDisposed(); }

	public void stopListening() {
		if (Boolean.TRUE.equals(isListening())) {
			log.info("Stopping LedgerListener...");
			listener.dispose();
		}
	}
}