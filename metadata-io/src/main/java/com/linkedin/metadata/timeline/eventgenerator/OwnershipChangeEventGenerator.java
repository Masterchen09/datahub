package com.linkedin.metadata.timeline.eventgenerator;

import static com.linkedin.metadata.Constants.*;

import com.datahub.util.RecordUtils;
import com.github.fge.jsonpatch.JsonPatch;
import com.linkedin.common.AuditStamp;
// import com.linkedin.common.Owner;
import com.linkedin.common.Ownership;
import com.linkedin.common.OwnershipType;
import com.linkedin.common.urn.Urn;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.entity.EntityAspect;
import com.linkedin.metadata.timeline.data.ChangeCategory;
import com.linkedin.metadata.timeline.data.ChangeEvent;
import com.linkedin.metadata.timeline.data.ChangeOperation;
import com.linkedin.metadata.timeline.data.ChangeTransaction;
import com.linkedin.metadata.timeline.data.SemanticChangeType;
import com.linkedin.metadata.timeline.data.entity.OwnerChangeEvent;
import com.linkedin.util.Pair;
import java.util.ArrayList;
import java.util.Comparator;
// import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class OwnershipChangeEventGenerator extends EntityChangeEventGenerator<Ownership> {
  private static final String OWNER_ADDED_FORMAT = "'%s' added as a '%s' of '%s'.";
  private static final String OWNER_REMOVED_FORMAT = "'%s' removed as a '%s' of '%s'.";
  // private static final String OWNERSHIP_TYPE_ADDED_FORMAT = "'%s' added as a
  // '%s' of '%s'.";
  // private static final String OWNERSHIP_TYPE_REMOVED_FORMAT = "'%s' removed as
  // a '%s' of '%s'.";

  private static List<ChangeEvent> computeDiffs(
      Ownership baseOwnership,
      Ownership targetOwnership,
      @Nonnull String entityUrn,
      AuditStamp auditStamp) {
    List<ChangeEvent> changeEvents = new ArrayList<>();

    // HashSet<Urn> baseOwnerUrns = (baseOwnership != null)
    // ? new HashSet<>(
    // baseOwnership.getOwners().stream()
    // .map(Owner::getOwner)
    // .collect(Collectors.toCollection(HashSet::new)))
    // : new HashSet<>();

    // HashSet<Urn> targetGlobalTagUrns = (targetOwnership != null)
    // ? new HashSet<>(
    // targetOwnership.getOwners().stream()
    // .map(Owner::getOwner)
    // .collect(Collectors.toCollection(HashSet::new)))
    // : new HashSet<>();

    HashSet<Pair<Urn, Urn>> baseOwners = (baseOwnership != null)
        ? new HashSet<>(
            baseOwnership.getOwners().stream()
                .map(
                    owner -> {
                      Urn ownerTypeUrn = owner.getTypeUrn();
                      if (ownerTypeUrn == null) {
                        ownerTypeUrn = mapOwnershipTypeToEntity(owner.getType().name());
                      }

                      return Pair.of(owner.getOwner(), ownerTypeUrn);
                    })
                .collect(Collectors.toCollection(HashSet::new)))
        : new HashSet<>();

    HashSet<Pair<Urn, Urn>> targetOwners = (targetOwnership != null)
        ? new HashSet<>(
            targetOwnership.getOwners().stream()
                .map(
                    owner -> {
                      Urn ownerTypeUrn = owner.getTypeUrn();
                      if (ownerTypeUrn == null) {
                        ownerTypeUrn = mapOwnershipTypeToEntity(owner.getType().name());
                      }

                      return Pair.of(owner.getOwner(), ownerTypeUrn);
                    })
                .collect(Collectors.toCollection(HashSet::new)))
        : new HashSet<>();

    // HashMap<Urn, HashSet<Urn>> baseOwnersMap = new HashMap<>();
    // if (baseOwnership != null) {
    // baseOwnership
    // .getOwners()
    // .forEach(
    // (owner) -> {
    // var ownerUrn = owner.getOwner();
    // if (!baseOwnersMap.containsKey(ownerUrn)) {
    // baseOwnersMap.put(ownerUrn, new HashSet<>());
    // }

    // Urn ownerTypeUrn = owner.getTypeUrn();
    // if (ownerTypeUrn == null) {
    // ownerTypeUrn = Urn.createFromTuple(
    // Constants.OWNERSHIP_TYPE_ENTITY_NAME,
    // SYSTEM_ID + owner.getType().name().toLowerCase());
    // }

    // baseOwnersMap.get(ownerUrn).add(ownerTypeUrn);
    // });
    // }

    // HashMap<Urn, HashSet<Urn>> targetOwnersMap = new HashMap<>();
    // if (targetOwnership != null) {
    // targetOwnership
    // .getOwners()
    // .forEach(
    // (owner) -> {
    // var ownerUrn = owner.getOwner();
    // if (!targetOwnersMap.containsKey(ownerUrn)) {
    // targetOwnersMap.put(ownerUrn, new HashSet<>());
    // }

    // Urn ownerTypeUrn = owner.getTypeUrn();
    // if (ownerTypeUrn == null) {
    // ownerTypeUrn = Urn.createFromTuple(
    // Constants.OWNERSHIP_TYPE_ENTITY_NAME,
    // SYSTEM_ID + owner.getType().name().toLowerCase());
    // }

    // targetOwnersMap.get(ownerUrn).add(ownerTypeUrn);
    // });
    // }

    baseOwners.stream()
        .filter(owner -> !targetOwners.contains(owner))
        .sorted(
            Comparator.comparing((Pair<Urn, Urn> owner) -> owner.getFirst().toString())
                .thenComparing(Comparator.comparing(owner -> owner.getSecond().toString())))
        .forEach(
            (owner) -> {
              Urn ownerUrn = owner.getFirst();
              Urn ownerTypeUrn = owner.getSecond();

              changeEvents.add(
                  OwnerChangeEvent.entityOwnerChangeEventBuilder()
                      .modifier(ownerUrn.toString())
                      .entityUrn(entityUrn)
                      .category(ChangeCategory.OWNER)
                      .operation(ChangeOperation.REMOVE)
                      .semVerChange(SemanticChangeType.MINOR)
                      .description(
                          String.format(
                              OWNER_REMOVED_FORMAT,
                              ownerUrn.getId(),
                              ownerTypeUrn.getId(),
                              entityUrn))
                      .ownerUrn(ownerUrn)
                      .ownerType(OwnershipType.CUSTOM)
                      .ownerTypeUrn(ownerTypeUrn)
                      .auditStamp(auditStamp)
                      .build());
            });

    targetOwners.stream()
        .filter(owner -> !baseOwners.contains(owner))
        .sorted(
            Comparator.comparing((Pair<Urn, Urn> owner) -> owner.getFirst().toString())
                .thenComparing(Comparator.comparing(owner -> owner.getSecond().toString())))
        .forEach(
            (owner) -> {
              Urn ownerUrn = owner.getFirst();
              Urn ownerTypeUrn = owner.getSecond();

              changeEvents.add(
                  OwnerChangeEvent.entityOwnerChangeEventBuilder()
                      .modifier(ownerUrn.toString())
                      .entityUrn(entityUrn)
                      .category(ChangeCategory.OWNER)
                      .operation(ChangeOperation.ADD)
                      .semVerChange(SemanticChangeType.MINOR)
                      .description(
                          String.format(
                              OWNER_ADDED_FORMAT,
                              ownerUrn.getId(),
                              ownerTypeUrn.getId(),
                              entityUrn))
                      .ownerUrn(ownerUrn)
                      .ownerType(OwnershipType.CUSTOM)
                      .ownerTypeUrn(ownerTypeUrn)
                      .auditStamp(auditStamp)
                      .build());
            });

    // baseOwnersMap.keySet().stream()
    // .filter(ownerUrn -> targetOwnersMap.containsKey(ownerUrn))
    // .sorted(Comparator.comparing(Urn::toString))
    // .forEach(ownerUrn -> {
    // HashSet<Urn> baseOwnerTypeUrns = baseOwnersMap.get(ownerUrn);
    // HashSet<Urn> targetOwnerTypeUrns = targetOwnersMap.get(ownerUrn);

    // baseOwnerTypeUrns.stream()
    // .filter(ownerTypeUrn -> !targetOwnerTypeUrns.contains(ownerTypeUrn))
    // .sorted(Comparator.comparing(Urn::toString))
    // .forEach(ownerTypeUrn -> {
    // changeEvents.add(
    // OwnerChangeEvent.entityOwnerChangeEventBuilder()
    // .modifier(ownerTypeUrn.toString())
    // .entityUrn(entityUrn)
    // .category(ChangeCategory.OWNER)
    // .operation(ChangeOperation.MODIFY)
    // .semVerChange(SemanticChangeType.PATCH)
    // .description(
    // String.format(
    // OWNERSHIP_TYPE_ADDED_FORMAT,
    // ownerUrn.getId(),
    // ownerTypeUrn.getId(),
    // entityUrn))
    // .ownerUrn(ownerUrn)
    // .ownerType(OwnershipType.CUSTOM)
    // .ownerTypeUrn(ownerTypeUrn)
    // .auditStamp(auditStamp)
    // .build());
    // });

    // targetOwnerTypeUrns.stream()
    // .filter(ownerTypeUrn -> baseOwnerTypeUrns.contains(ownerTypeUrn))
    // .sorted(Comparator.comparing(Urn::toString))
    // .forEach(ownerTypeUrn -> {
    // changeEvents.add(
    // OwnerChangeEvent.entityOwnerChangeEventBuilder()
    // .modifier(ownerTypeUrn.toString())
    // .entityUrn(entityUrn)
    // .category(ChangeCategory.OWNER)
    // .operation(ChangeOperation.MODIFY)
    // .semVerChange(SemanticChangeType.PATCH)
    // .description(
    // String.format(
    // OWNERSHIP_TYPE_REMOVED_FORMAT,
    // ownerUrn.getId(),
    // ownerTypeUrn.getId(),
    // entityUrn))
    // .ownerUrn(ownerUrn)
    // .ownerType(OwnershipType.CUSTOM)
    // .ownerTypeUrn(ownerTypeUrn)
    // .auditStamp(auditStamp)
    // .build());
    // });
    // });

    return changeEvents;
  }

  private static Ownership getOwnershipFromAspect(EntityAspect entityAspect) {
    if (entityAspect != null && entityAspect.getMetadata() != null) {
      return RecordUtils.toRecordTemplate(Ownership.class, entityAspect.getMetadata());
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

    if (!previousValue.getAspect().equals(OWNERSHIP_ASPECT_NAME)
        || !currentValue.getAspect().equals(OWNERSHIP_ASPECT_NAME)) {
      throw new IllegalArgumentException("Aspect is not " + OWNERSHIP_ASPECT_NAME);
    }

    Ownership baseOwnership = getOwnershipFromAspect(previousValue);
    Ownership targetOwnership = getOwnershipFromAspect(currentValue);

    List<ChangeEvent> changeEvents = new ArrayList<>();
    if (element == ChangeCategory.OWNER) {
      changeEvents.addAll(
          computeDiffs(baseOwnership, targetOwnership, currentValue.getUrn(), null));
    }

    // Assess the highest change at the transaction(schema) level.
    // Why isn't this done at changeevent level - what if transaction contains
    // multiple category
    // events?
    SemanticChangeType highestSemanticChange = SemanticChangeType.NONE;
    ChangeEvent highestChangeEvent = changeEvents.stream().max(Comparator.comparing(ChangeEvent::getSemVerChange))
        .orElse(null);
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
      @Nonnull Aspect<Ownership> from,
      @Nonnull Aspect<Ownership> to,
      @Nonnull AuditStamp auditStamp) {
    return computeDiffs(from.getValue(), to.getValue(), urn.toString(), auditStamp);
  }

  private static Urn mapOwnershipTypeToEntity(String type) {
    final String typeName = SYSTEM_ID + type.toLowerCase();
    return Urn.createFromTuple(Constants.OWNERSHIP_TYPE_ENTITY_NAME, typeName);
  }
}
