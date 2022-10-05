package com.lashe.example.api.helper;

import com.lashe.example.api.common.exceptions.ApiException;
import com.lashe.example.api.common.daml.LedgerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

/**
 * Helper to deal with Ledger DAML models
 */
@Slf4j
public final class LedgerHelper {
	public static Mono<Boolean> connect(LedgerClient ledgerClient) {
		return ledgerClient.isConnected()
				.map(connected -> {
					try {
						if (Boolean.FALSE.equals(connected)) {
							log.info("Trying to connect Ledger Client to Server: {}", ledgerClient.getServerAddress());
							ledgerClient.connect();
							log.info("Ledger Client connect successfully.");
						}
						return true;
					} catch (Exception ex) {
						log.error("Unable to connect to the Ledger Server", ex);
						throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not connect to the Ledger Server, please see logs for details.");
					}
				});
	}

	/**
	 * Try to extract DAML error code information
	 */
	public static String extractErrorCode(String message) {
		String fromString = "StatusRuntimeException: ";
		int from = message.indexOf(fromString) + fromString.length();
		int to = message.indexOf(": Command");
		
		if (from > 0 && to > 0) {
			return message.substring(from, to);
		} else {
			return HttpStatus.BAD_REQUEST.name();
		}	
	}
	
	/**
	 * Try to extract DAML error information in a more readable and accurate way
	 */
	public static String extractErrorMessage(String message) {
		String fromString = "Error: ";
		int from = message.indexOf(fromString) + fromString.length();
		int to = message.indexOf("Details:");
		
		if (from > 0 && to > 0) {
			return message.substring(from, to);
		} else {
			return message;
		}	
	}

	private LedgerHelper() { throw new IllegalStateException("Utility class."); }
}
