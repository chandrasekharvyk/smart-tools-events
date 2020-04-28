package com.mrll.javelin.api.smarttools.publisher;

import com.mrll.javelin.api.smarttools.publisher.model.CategorizationEvent;
import com.mrll.javelin.common.event.mq.JavelinMessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CategorizationPublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategorizationPublisher.class);

    private JavelinMessageTemplate javelinMessageTemplate;

    @Value("${web-event.smartCategorizationResultExchange}")
    private String smartCategorizationResultExchange;

    @Value("${web-event.smartCategorizationResultRoutingKey}")
    private String smartCategorizationResultRoutingKey;

    @Autowired
    public CategorizationPublisher(JavelinMessageTemplate javelinMessageTemplate) {
        this.javelinMessageTemplate = javelinMessageTemplate;
    }

    public void publishCategorizationEvent(CategorizationEvent categorizationEvent) {

        javelinMessageTemplate.sendObjectMessage(
                smartCategorizationResultExchange,
                smartCategorizationResultRoutingKey,
                categorizationEvent
        );
        LOGGER.info("message=publishedCategorizationEvent projectId={} metadataId={} categorizationEvent={}", categorizationEvent.getProjectId(), categorizationEvent.getMetadataId(), categorizationEvent);
    }
}
