package com.mrll.javelin.api.smarttools.publisher;

import com.mrll.javelin.api.smarttools.model.delegates.Prediction;
import com.mrll.javelin.api.smarttools.publisher.model.CategoryUpdateEvent;
import com.mrll.javelin.common.event.mq.JavelinMessageTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CategoryUpdatePublisher {
    private static final Logger LOGGER = LoggerFactory.getLogger(CategoryUpdatePublisher.class);

    private JavelinMessageTemplate javelinMessageTemplate;

    @Value("${message.smartInteractionExchange}")
    private String smartInteractionExchange;

    @Value("${message.smartInteractionUpdateCategoryRoutingKey}")
    private String smartInteractionUpdateCategoryRoutingKey;

    @Autowired
    public CategoryUpdatePublisher(JavelinMessageTemplate javelinMessageTemplate) {
        this.javelinMessageTemplate = javelinMessageTemplate;
    }

    public void publishCategoryUpdate(String projectId, String metadataId, List<Prediction> oldCategories, List<Prediction> updatedCategories) {
        CategoryUpdateEvent categoryUpdateEvent = new CategoryUpdateEvent();
        categoryUpdateEvent.setProjectId(projectId);
        categoryUpdateEvent.setMetadataId(metadataId);
        categoryUpdateEvent.setOldCategories(oldCategories);
        categoryUpdateEvent.setUpdatedCategories(updatedCategories);

        javelinMessageTemplate.sendObjectMessage(
                smartInteractionExchange,
                smartInteractionUpdateCategoryRoutingKey,
                categoryUpdateEvent
        );
        LOGGER.info("message=publishedCategoryUpdateEvent projectId={} metadataId={} updatedCategories={}", projectId, metadataId, updatedCategories);
    }
}
