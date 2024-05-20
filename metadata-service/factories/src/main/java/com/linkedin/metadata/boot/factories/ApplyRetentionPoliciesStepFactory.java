package com.linkedin.metadata.boot.factories;

import com.linkedin.gms.factory.entity.RetentionServiceFactory;
import com.linkedin.metadata.boot.steps.ApplyRetentionPoliciesStep;
import com.linkedin.metadata.entity.RetentionService;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

@Configuration
@Import({RetentionServiceFactory.class})
public class ApplyRetentionPoliciesStepFactory {

  @Autowired
  @Qualifier("retentionService")
  private RetentionService _retentionService;

  @Value("${entityService.retention.enabled}")
  private Boolean _enableRetention;

  @Value("${entityService.retention.applyOnBootstrap}")
  private Boolean _applyOnBootstrap;

  @Bean(name = "applyRetentionPoliciesStep")
  @Scope("singleton")
  @Nonnull
  protected ApplyRetentionPoliciesStep createInstance() {
    return new ApplyRetentionPoliciesStep(
        _retentionService,
        _enableRetention,
        _applyOnBootstrap);
  }
}
