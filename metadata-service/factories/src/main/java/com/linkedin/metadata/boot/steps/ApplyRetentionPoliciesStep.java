package com.linkedin.metadata.boot.steps;

import static com.linkedin.metadata.Constants.*;

import com.datahub.util.RecordUtils;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.linkedin.common.urn.Urn;
import com.linkedin.metadata.boot.BootstrapStep;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.entity.RetentionService;
import com.linkedin.metadata.key.DataHubRetentionKey;
import com.linkedin.retention.DataHubRetentionConfig;
import io.datahubproject.metadata.context.OperationContext;
import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@RequiredArgsConstructor
public class ApplyRetentionPoliciesStep implements BootstrapStep {

  private final RetentionService<?> _retentionService;
  private final boolean _enableRetention;
  private final boolean _applyOnBootstrap;

  @Nonnull
  @Override
  public ExecutionMode getExecutionMode() {
    return ExecutionMode.ASYNC;
  }

  @Override
  public String name() {
    return "IngestRetentionPoliciesStep";
  }

  @Override
  public void execute(@Nonnull OperationContext systemOperationContext)
      throws IOException, URISyntaxException {
    // If retention is disabled, skip step
    if (!_enableRetention) {
      log.info("ApplyRetentionPolicies disabled. Skipping.");
      return;
    }

    if (_applyOnBootstrap) {
      log.info("Applying policies to all records");
      _retentionService.batchApplyRetention(null, null);
    }
  }
}
