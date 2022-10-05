package com.lashe.example.api.configuration;

import com.lashe.example.api.common.daml.LedgerClient;
import com.lashe.example.api.configuration.properties.LedgerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class LedgerConfig {
	private final LedgerProperties ledgerProperties;
	
	@Bean
	public LedgerClient newLedgerClient() {
		LedgerClient ledgerClient = LedgerClient
				.newBuilder(ledgerProperties.getHost(), ledgerProperties.getPort())
				.withExpectedLedgerId(ledgerProperties.getLedgerId())
				.build();

		log.info("LedgerClient created for Ledger Server {}:{}", ledgerProperties.getHost(),
				ledgerProperties.getPort());

		return ledgerClient;
	}
}