package com.example.api.configuration.properties;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "ledger")
public class LedgerProperties {
	private String host;

	private int port;

	private String ledgerId;

	private String custodian;

	private int maxAttempts;

	private int backoffSeconds;

	private BiMap<String, String> ledgerPartyIdentifierMap = HashBiMap.create();

	public String getLedgerDisplayName(String partyId) { return ledgerPartyIdentifierMap.getOrDefault(partyId, partyId); }

	public String getLedgerIdFromDisplayName(String displayName) { return ledgerPartyIdentifierMap.inverse().getOrDefault(displayName, displayName); }

	public String getCustodianPartyId() { return getPartyId(custodian); }

	private String getPartyId(String displayName) {
		return ledgerPartyIdentifierMap.inverse().getOrDefault(displayName, displayName);
	}
}
