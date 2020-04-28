package com.mrll.javelin.api.smarttools.publisher;

import com.mrll.javelin.api.smarttools.model.delegates.Prediction;
import com.mrll.javelin.api.smarttools.mongo.entity.SmartDoc;
import com.mrll.javelin.common.audit.constants.SmartCategoryActivityType;
import com.mrll.javelin.common.audit.model.external.datasiteone.smartcategory.Category;
import com.mrll.javelin.common.audit.model.external.datasiteone.smartcategory.CategoryAuditRequest;
import com.mrll.javelin.common.audit.service.AuditPublishingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class AuditingPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(AuditingPublisher.class);
    private AuditPublishingService auditPublishingService;

    @Autowired
    public AuditingPublisher(AuditPublishingService auditPublishingService) {
        this.auditPublishingService = auditPublishingService;
    }

    /**
     * @param smartDoc
     * @param newCategories
     */
    @Async
    public void publishAuditCategories(SmartDoc smartDoc, List<Prediction> newCategories) {

        CategoryAuditRequest categoryAuditRequest = new CategoryAuditRequest();
        categoryAuditRequest.setMetadataId(smartDoc.getMetadataId());
        categoryAuditRequest.setProjectId(smartDoc.getProjectId());
        categoryAuditRequest.setBlobId(smartDoc.getBlobId());

        if (newCategories != null && newCategories.size() > 0) {
            categoryAuditRequest.setSmartCategoryActivityType(SmartCategoryActivityType.SMART_CATEGORIES_UPDATED);
            categoryAuditRequest.setPreviousCategories(smartDoc.getCategories().stream()
                    .map(categories -> new Category().setCategoryName(categories.getLabel()).setProbability(categories.getProbability()))
                    .collect(Collectors.toList()));
            categoryAuditRequest.setCategories(newCategories.stream()
                    .map(categories -> new Category().setCategoryName(categories.getLabel()).setProbability(categories.getProbability()))
                    .collect(Collectors.toList()));
            LOG.info("method=publishAuditCategories, message=publishCategoriesUpdateEvent for project={} metadataId={} with categories= {} updatedCategories={} event={}",
                    smartDoc.getProjectId(),
                    smartDoc.getMetadataId(),
                    smartDoc.getCategories().stream().map(category -> category.getLabel()).collect(Collectors.toList()),
                    newCategories.stream().map(categories -> categories.getLabel()).collect(Collectors.toList()),
                    categoryAuditRequest.getSmartCategoryActivityType());
        } else {
            categoryAuditRequest.setCategories(smartDoc.getCategories().stream()
                    .map(categories -> new Category().setCategoryName(categories.getLabel()).setProbability(categories.getProbability()))
                    .collect(Collectors.toList()));
            categoryAuditRequest.setSmartCategoryActivityType(SmartCategoryActivityType.SMART_CATEGORIES_CREATED);
            LOG.info("method=publishAuditCategories, message=publishCategoriesCreatedEvent for project={} metadataId={} with categories= {} event={}",
                    smartDoc.getProjectId(), smartDoc.getMetadataId(),
                    smartDoc.getCategories().stream().map(categories -> categories.getLabel()).collect(Collectors.toList()),
                    categoryAuditRequest.getSmartCategoryActivityType());
        }

        auditPublishingService.publishSmartCategoryEvent(categoryAuditRequest);

        LOG.info("message=publishedCategorizationEvent projectId={} metadataId={} categorizationEvent={}",
                categoryAuditRequest.getProjectId(), categoryAuditRequest.getMetadataId(), categoryAuditRequest.getSmartCategoryActivityType());
    }
}
