package com.lashe.example.api.common.exceptions;

import com.lashe.example.api.common.AppConstants;
import io.grpc.StatusRuntimeException;
import org.checkerframework.com.google.common.collect.Lists;
import org.checkerframework.com.google.common.collect.Maps;
import org.slf4j.MDC;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.List;
import java.util.Map;

/**
 * Global handler to customize response errors in reactive controllers.
 */
@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {
	private static final String STATUS = "status";
	private static final String MESSAGE = "message";
	private static final String CODE = "code";

	/**
	 * It returns a simplified view of the error in order to hide e.g. extra java
	 * information(sensitive or not)
	 */
	@Override
	public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions errorAttributeOptions) {

		String traceId = MDC.get("traceId");
		Throwable error = getError(request);
		if (error instanceof WebExchangeBindException) {
			WebExchangeBindException ex = (WebExchangeBindException) error;

			Map<String, Object> map = Maps.newHashMap();
			map.put(STATUS, ex.getStatus().value());
			map.put(MESSAGE, ex.getReason());

			List<String> errors = Lists.newArrayList();
			ex.getBindingResult().getGlobalErrors().forEach(err -> errors.add(err.getDefaultMessage()));

			ex.getBindingResult().getFieldErrors().forEach(field ->
					errors.add("The field " + field.getField() + " " + field.getDefaultMessage())
			);
			
			if(!errors.isEmpty()) { map.put("errors", errors); }

			return map;
		} else if (error instanceof ApiException) {
			ApiException ex = (ApiException) error;

			Map<String, Object> map = Maps.newHashMap();
			map.put(STATUS, ex.getStatus().value());
			map.put(CODE, ex.getCode());
			map.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, ex.getMessage()));

			return map;
		} else if (error instanceof StatusRuntimeException) {
			StatusRuntimeException ex = (StatusRuntimeException) error;

			Map<String, Object> map = Maps.newHashMap();
			map.put(STATUS, ex.getStatus());
			map.put(MESSAGE, String.format(AppConstants.ERROR_MESSAGE_FORMAT, traceId, ex.getMessage()));

			return map;			
		} else {
			return super.getErrorAttributes(request, ErrorAttributeOptions.of(ErrorAttributeOptions.Include.STACK_TRACE));
		}
	}
}