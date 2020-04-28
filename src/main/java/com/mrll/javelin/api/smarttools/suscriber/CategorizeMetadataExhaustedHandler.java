package com.mrll.javelin.api.smarttools.suscriber;

import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.common.event.mq.exhaustion.RetriesExhaustedHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class CategorizeMetadataExhaustedHandler implements RetriesExhaustedHandler<MetadataUpdateEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(CategorizeMetadataExhaustedHandler.class);

    @Override
    public void handleRetriesExhausted(MetadataUpdateEvent message, Map<String, Object> headers) {
        LOG.debug("message=maxRetriesExhausted projectId={}, metadataIds={}, originalStacktrace={}",
                message.getProjectId(), message.getMetadataIds(), headers.get(STACKTRACE_HEADER));
    }
}
