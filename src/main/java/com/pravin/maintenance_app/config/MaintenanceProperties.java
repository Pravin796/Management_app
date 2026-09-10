package com.pravin.maintenance_app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@Getter
@Setter
@ConfigurationProperties(prefix = "maintenance")
public class MaintenanceProperties {

    private BigDecimal monthlyAmount;
}