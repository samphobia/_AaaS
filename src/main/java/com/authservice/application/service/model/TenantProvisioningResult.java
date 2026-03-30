package com.authservice.application.service.model;

import com.authservice.domain.model.CustomAttributeDefinition;
import com.authservice.domain.model.Tenant;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class TenantProvisioningResult {
    Tenant tenant;
    List<CustomAttributeDefinition> attributeDefinitions;
}
