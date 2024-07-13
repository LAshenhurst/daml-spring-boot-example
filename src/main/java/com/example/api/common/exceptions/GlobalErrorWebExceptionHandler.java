package com.example.api.common.exceptions;

import com.example.api.common.AppConstants;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
	private static final String STATUS = "status";
	private static final String CODE = "code";
	private static final String MESSAGE = "message";

	private static final ServerCodecConfigurer SERVER_CODEC_CONFIGURER = ServerCodecConfigurer.create();

	public GlobalErrorWebExceptionHandler(ErrorAttributes g, ApplicationContext applicationContext) {
		super(g, new WebProperties.Resources(), applicationContext);
		super.setMessageWriters(SERVER_CODEC_CONFIGURER.getWriters());
		super.setMessageReaders(SERVER_CODEC_CONFIGURER.getReaders());
	}

	@Override
	protected RouterFunction<ServerResponse> getRoutingFunction(final ErrorAttributes errorAttributes) {
		return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
	}

	private Mono<ServerResponse> renderErrorResponse(final ServerRequest request) {
		ErrorAttributeOptions errorAttributeOptions = ErrorAttributeOptions.of(
				ErrorAttributeOptions.Include.MESSAGE,
				ErrorAttributeOptions.Include.EXCEPTION
		);

		final Map<String, Object> rawErrors = getErrorAttributes(request, errorAttributeOptions);
		final Map<String, Object> returnErrors = processErrors(rawErrors);

		return ServerResponse
				.status(HttpStatus.valueOf((int) returnErrors.get(STATUS)))
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(processErrors(rawErrors)));
	}

	/**
	 * @param errors raw error information from the request
	 * @return simplified view of the errors to hide extra information (sensitive or not)
	 */
	private Map<String, Object> processErrors(Map<String, Object> errors) {
		Map<String, Object> responseMap = new HashMap<>();

		// if error is from our service then it is already well formatted and can be returned
		boolean alreadyFormatted = errors.keySet().equals(Set.of(STATUS, MESSAGE, CODE));
		if (alreadyFormatted) { return errors; }

		// if error is from spring component then some of these fields should be filled, default to empty string to avoid NPE
		String exception = (String) errors.getOrDefault("exception", "");
		String message = (String) errors.getOrDefault(MESSAGE, "");
		String error = (String) errors.getOrDefault("error", "");
		String trace = (String) errors.getOrDefault("trace", "");

		String traceId = MDC.get("traceId");

		if (exception.contains("DecodingException") || trace.contains("DecodingException")) {
			responseMap.put(STATUS, 400);
			responseMap.put(CODE, "400");
			responseMap.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, "Invalid request, unable to decode request"));
		} else if (error.equalsIgnoreCase("Method Not Allowed")) {
			responseMap.put(STATUS, 405);
			responseMap.put(CODE, "405");
			responseMap.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, "Method Not Allowed"));
		} else if (error.equalsIgnoreCase("Not Found")) {
			responseMap.put(STATUS, 404);
			responseMap.put(CODE, "404");
			responseMap.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, "URL not found"));
		} else if (message.contains("Request body is missing")) {
			responseMap.put(STATUS, 400);
			responseMap.put(CODE, "400");
			responseMap.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, "Request body was expected but not received"));
		} else if (trace.contains("Required request part 'file' is not present")) {
			responseMap.put(STATUS, 400);
			responseMap.put(CODE, "400");
			responseMap.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, "Expected this request to upload a file, but no file found."));
		} else if (message.equals("Validation failure")) {
			List<String> validationFailures = (List) errors.get("errors");
			responseMap.put(STATUS, 400);
			responseMap.put(CODE, "400");
			responseMap.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, "Validation failure - " + String.join(",", validationFailures)));
		} else {
			log.warn("Handling unexpected exception, returning default message. Exception: {}", errors);
			responseMap.put(STATUS, 500);
			responseMap.put(CODE, "500");
			responseMap.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, AppConstants.DEFAULT_ERROR_MESSAGE));
		}

		return responseMap;
	}
}