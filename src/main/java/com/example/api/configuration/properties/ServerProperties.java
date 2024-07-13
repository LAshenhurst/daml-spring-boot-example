package com.example.api.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotEmpty;

@Configuration
@ConfigurationProperties(prefix = "server")
@Data
public class ServerProperties {
	@NotEmpty
	private String appId;
}