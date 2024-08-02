package com.linkedin.metadata.timeline.eventgenerator;

import static com.linkedin.metadata.Constants.*;

import com.datahub.util.RecordUtils;
import com.github.fge.jsonpatch.JsonPatch;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.GlossaryTermAssociation;
import com.linkedin.common.GlossaryTerms;
import com.linkedin.common.urn.Urn;
import com.linkedin.metadata.entity.EntityAspect;
import com.linkedin.metadata.timeline.data.ChangeCategory;
import com.linkedin.metadata.timeline.data.ChangeEvent;
import com.linkedin.metadata.timeline.data.ChangeOperation;
import com.linkedin.metadata.timeline.data.ChangeTransaction;
import com.linkedin.metadata.timeline.data.SemanticChangeType;
import com.linkedin.metadata.timeline.data.entity.GlossaryTermChangeEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class GlossaryTermsChangeEventGenerator extends EntityChangeEventGenerator<GlossaryTerms> {
  private static final String GLOSSARY_TERM_ADDED_FORMAT = "Term '%s' added to entity '%s'.";
  private static final String GLOSSARY_TERM_REMOVED_FORMAT = "Term '%s' removed from entity '%s'.";

  public static List<ChangeEvent> computeDiffs(
      GlossaryTerms baseGlossaryTerms,
      GlossaryTerms targetGlossaryTerms,
      @Nonnull String entityUrn,
      AuditStamp auditStamp) {
    List<ChangeEvent> changeEvents = new ArrayList<>();

    HashSet<Urn> baseGlossaryTermUrns =
        (baseGlossaryTerms != null)
            ? new HashSet<>(
                baseGlossaryTerms.getTerms().stream()
                    .map(GlossaryTermAssociation::getUrn)
                    .collect(Collectors.toCollection(HashSet::new)))
            : new HashSet<>();

    HashSet<Urn> targetGlossaryTermUrns =
        (targetGlossaryTerms != null)
            ? new HashSet<>(
                targetGlossaryTerms.getTerms().stream()
                    .map(GlossaryTermAssociation::getUrn)
                    .collect(Collectors.toCollection(HashSet::new)))
            : new HashSet<>();

    baseGlossaryTermUrns.stream()
        .filter(termAssociation -> !targetGlossaryTermUrns.contains(termAssociation))
        .sorted(Comparator.comparing(Urn::toString))
        .forEach(
            (termUrn) -> {
              changeEvents.add(
                  GlossaryTermChangeEvent.entityGlossaryTermChangeEventBuilder()
                      .modifier(termUrn.toString())
                      .entityUrn(entityUrn)
                      .category(ChangeCategory.GLOSSARY_TERM)
                      .operation(ChangeOperation.REMOVE)
                      .semVerChange(SemanticChangeType.MINOR)
                      .description(
                          String.format(GLOSSARY_TERM_REMOVED_FORMAT, termUrn.getId(), entityUrn))
                      .termUrn(termUrn)
                      .auditStamp(auditStamp)
                      .build());
            });

    targetGlossaryTermUrns.stream()
        .filter(termUrn -> !baseGlossaryTermUrns.contains(termUrn))
        .sorted(Comparator.comparing(Urn::toString))
        .forEach(
            (termUrn) -> {
              changeEvents.add(
                  GlossaryTermChangeEvent.entityGlossaryTermChangeEventBuilder()
                      .modifier(termUrn.toString())
                      .entityUrn(entityUrn)
                      .category(ChangeCategory.GLOSSARY_TERM)
                      .operation(ChangeOperation.ADD)
                      .semVerChange(SemanticChangeType.MINOR)
                      .description(
                          String.format(GLOSSARY_TERM_ADDED_FORMAT, termUrn.getId(), entityUrn))
                      .termUrn(termUrn)
                      .auditStamp(auditStamp)
                      .build());
            });

    return changeEvents;
  }

  private static GlossaryTerms getGlossaryTermsFromAspect(EntityAspect entityAspect) {
    if (entityAspect != null && entityAspect.getMetadata() != null) {
      return RecordUtils.toRecordTemplate(GlossaryTerms.class, entityAspect.getMetadata());
    }
    return null;
  }

  @Override
  public ChangeTransaction getSemanticDiff(
      EntityAspect previousValue,
      EntityAspect currentValue,
      ChangeCategory element,
      JsonPatch rawDiff,
      boolean rawDiffsRequested) {

    if (currentValue == null) {
      throw new IllegalArgumentException("EntityAspect currentValue should not be null");
    }

    if (!previousValue.getAspect().equals(GLOSSARY_TERMS_ASPECT_NAME)
        || !currentValue.getAspect().equals(GLOSSARY_TERMS_ASPECT_NAME)) {
      throw new IllegalArgumentException("Aspect is not " + GLOSSARY_TERMS_ASPECT_NAME);
    }

    GlossaryTerms baseGlossaryTerms = getGlossaryTermsFromAspect(previousValue);
    GlossaryTerms targetGlossaryTerms = getGlossaryTermsFromAspect(currentValue);
    List<ChangeEvent> changeEvents = new ArrayList<>();
    if (element == ChangeCategory.GLOSSARY_TERM) {
      changeEvents.addAll(
          computeDiffs(baseGlossaryTerms, targetGlossaryTerms, currentValue.getUrn(), null));
    }

    // Assess the highest change at the transaction(schema) level.
    SemanticChangeType highestSemanticChange = SemanticChangeType.NONE;
    ChangeEvent highestChangeEvent =
        changeEvents.stream().max(Comparator.comparing(ChangeEvent::getSemVerChange)).orElse(null);
    if (highestChangeEvent != null) {
      highestSemanticChange = highestChangeEvent.getSemVerChange();
    }

    return ChangeTransaction.builder()
        .semVerChange(highestSemanticChange)
        .changeEvents(changeEvents)
        .timestamp(currentValue.getCreatedOn().getTime())
        .rawDiff(rawDiffsRequested ? rawDiff : null)
        .actor(currentValue.getCreatedBy())
        .build();
  }

  @Override
  public List<ChangeEvent> getChangeEvents(
      @Nonnull Urn urn,
      @Nonnull String entity,
      @Nonnull String aspect,
      @Nonnull Aspect<GlossaryTerms> from,
      @Nonnull Aspect<GlossaryTerms> to,
      @Nonnull AuditStamp auditStamp) {
    return computeDiffs(from.getValue(), to.getValue(), urn.toString(), auditStamp);
  }
}
