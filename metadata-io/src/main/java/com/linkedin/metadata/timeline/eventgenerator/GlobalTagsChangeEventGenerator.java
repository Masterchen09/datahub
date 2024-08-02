package com.linkedin.metadata.timeline.eventgenerator;

import static com.linkedin.metadata.Constants.*;

import com.datahub.util.RecordUtils;
import com.github.fge.jsonpatch.JsonPatch;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.GlobalTags;
import com.linkedin.common.TagAssociation;
import com.linkedin.common.urn.Urn;
import com.linkedin.metadata.entity.EntityAspect;
import com.linkedin.metadata.timeline.data.ChangeCategory;
import com.linkedin.metadata.timeline.data.ChangeEvent;
import com.linkedin.metadata.timeline.data.ChangeOperation;
import com.linkedin.metadata.timeline.data.ChangeTransaction;
import com.linkedin.metadata.timeline.data.SemanticChangeType;
import com.linkedin.metadata.timeline.data.entity.TagChangeEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class GlobalTagsChangeEventGenerator extends EntityChangeEventGenerator<GlobalTags> {
  private static final String TAG_ADDED_FORMAT = "Tag '%s' added to entity '%s'.";
  private static final String TAG_REMOVED_FORMAT = "Tag '%s' removed from entity '%s'.";

  public static List<ChangeEvent> computeDiffs(
      GlobalTags baseGlobalTags,
      GlobalTags targetGlobalTags,
      @Nonnull String entityUrn,
      AuditStamp auditStamp) {
    List<ChangeEvent> changeEvents = new ArrayList<>();

    HashSet<Urn> baseGlobalTagUrns =
        (baseGlobalTags != null)
            ? new HashSet<>(
                baseGlobalTags.getTags().stream()
                    .map(TagAssociation::getTag)
                    .collect(Collectors.toCollection(HashSet::new)))
            : new HashSet<>();

    HashSet<Urn> targetGlobalTagUrns =
        (targetGlobalTags != null)
            ? new HashSet<>(
                targetGlobalTags.getTags().stream()
                    .map(TagAssociation::getTag)
                    .collect(Collectors.toCollection(HashSet::new)))
            : new HashSet<>();

    baseGlobalTagUrns.stream()
        .filter(globalTagUrn -> !targetGlobalTagUrns.contains(globalTagUrn))
        .sorted(Comparator.comparing(Urn::toString))
        .forEach(
            (globalTagUrn) -> {
              changeEvents.add(
                  TagChangeEvent.entityTagChangeEventBuilder()
                      .modifier(globalTagUrn.toString())
                      .entityUrn(entityUrn)
                      .category(ChangeCategory.TAG)
                      .operation(ChangeOperation.REMOVE)
                      .semVerChange(SemanticChangeType.MINOR)
                      .description(
                          String.format(TAG_REMOVED_FORMAT, globalTagUrn.getId(), entityUrn))
                      .tagUrn(globalTagUrn)
                      .auditStamp(auditStamp)
                      .build());
            });

    targetGlobalTagUrns.stream()
        .filter(globalTagUrn -> !baseGlobalTagUrns.contains(globalTagUrn))
        .sorted(Comparator.comparing(Urn::toString))
        .forEach(
            (globalTagUrn) -> {
              changeEvents.add(
                  TagChangeEvent.entityTagChangeEventBuilder()
                      .modifier(globalTagUrn.toString())
                      .entityUrn(entityUrn)
                      .category(ChangeCategory.TAG)
                      .operation(ChangeOperation.ADD)
                      .semVerChange(SemanticChangeType.MINOR)
                      .description(String.format(TAG_ADDED_FORMAT, globalTagUrn.getId(), entityUrn))
                      .tagUrn(globalTagUrn)
                      .auditStamp(auditStamp)
                      .build());
            });

    return changeEvents;
  }

  private static GlobalTags getGlobalTagsFromAspect(EntityAspect entityAspect) {
    if (entityAspect != null && entityAspect.getMetadata() != null) {
      return RecordUtils.toRecordTemplate(GlobalTags.class, entityAspect.getMetadata());
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
    if (!previousValue.getAspect().equals(GLOBAL_TAGS_ASPECT_NAME)
        || !currentValue.getAspect().equals(GLOBAL_TAGS_ASPECT_NAME)) {
      throw new IllegalArgumentException("Aspect is not " + GLOBAL_TAGS_ASPECT_NAME);
    }

    GlobalTags baseGlobalTags = getGlobalTagsFromAspect(previousValue);
    GlobalTags targetGlobalTags = getGlobalTagsFromAspect(currentValue);
    List<ChangeEvent> changeEvents = new ArrayList<>();
    if (element == ChangeCategory.TAG) {
      changeEvents.addAll(
          computeDiffs(baseGlobalTags, targetGlobalTags, currentValue.getUrn(), null));
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
      @Nonnull Aspect<GlobalTags> from,
      @Nonnull Aspect<GlobalTags> to,
      @Nonnull AuditStamp auditStamp) {
    return computeDiffs(from.getValue(), to.getValue(), urn.toString(), auditStamp);
  }
}
