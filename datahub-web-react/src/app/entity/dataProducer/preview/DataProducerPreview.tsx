import React from 'react';
import {
    Domain,
    Container,
    EntityType,
    GlobalTags,
    GlossaryTerms,
    Owner,
    SearchInsight,
    ParentContainersResult,
} from '../../../../types.generated';
import DefaultPreviewCard from '../../../preview/DefaultPreviewCard';
import { useEntityRegistry } from '../../../useEntityRegistry';
import { capitalizeFirstLetter } from '../../../shared/textUtil';

export const DataProducerPreview = ({
    urn,
    name,
    platformInstanceId,
    description,
    platform,
    owners,
    tags,
    glossaryTerms,
    domain,
    container,
    insights,
    logoUrl,
    parentContainers,
}: {
    urn: string;
    platform: string;
    platformInstanceId?: string;
    name?: string;
    description?: string | null;
    owners?: Array<Owner> | null;
    tags?: GlobalTags;
    glossaryTerms?: GlossaryTerms | null;
    domain?: Domain | null;
    container?: Container | null;
    insights?: Array<SearchInsight> | null;
    logoUrl?: string | null;
    parentContainers?: ParentContainersResult | null;
}): JSX.Element => {
    const entityRegistry = useEntityRegistry();
    const capitalizedPlatform = capitalizeFirstLetter(platform);

    return (
        <DefaultPreviewCard
            url={entityRegistry.getEntityUrl(EntityType.DataProducer, urn)}
            name={name || ''}
            description={description || ''}
            type="DataProducer"
            logoUrl={logoUrl || ''}
            platformInstanceId={platformInstanceId}
            platform={capitalizedPlatform}
            owners={owners}
            tags={tags}
            container={container || undefined}
            glossaryTerms={glossaryTerms || undefined}
            domain={domain}
            insights={insights}
            parentContainers={parentContainers}
        />
    );
};
