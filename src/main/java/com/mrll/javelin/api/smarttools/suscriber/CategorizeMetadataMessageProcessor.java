package com.mrll.javelin.api.smarttools.suscriber;

import com.mrll.javelin.api.smarttools.model.delegates.MetadataUpdateEvent;
import com.mrll.javelin.api.smarttools.service.CategorizationService;
import com.mrll.javelin.common.event.mq.exhaustion.MessageProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CategorizeMetadataMessageProcessor implements MessageProcessor<MetadataUpdateEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(CategorizeMetadataMessageProcessor.class);

    private CategorizationService categorizationService;

    public CategorizeMetadataMessageProcessor(CategorizationService categorizationService) {
        this.categorizationService = categorizationService;
    }

    @Override
    public void processMessage(MetadataUpdateEvent message) {

        LOG.info("method=processMessage, message=received msg from queue, msg={}", message);
        categorizationService.findCategoriesAndSuggestions(message);

    }
}
