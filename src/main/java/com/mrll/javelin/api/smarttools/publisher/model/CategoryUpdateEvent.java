package com.mrll.javelin.api.smarttools.publisher.model;

import com.mrll.javelin.api.smarttools.model.delegates.Prediction;

import java.util.List;

public class CategoryUpdateEvent {
    private String projectId;
    private String metadataId;
    private List<Prediction> oldCategories;
    private List<Prediction> updatedCategories;

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getMetadataId() {
        return metadataId;
    }

    public void setMetadataId(String metadataId) {
        this.metadataId = metadataId;
    }

    public List<Prediction> getOldCategories() {
        return oldCategories;
    }

    public void setOldCategories(List<Prediction> oldCategories) {
        this.oldCategories = oldCategories;
    }

    public List<Prediction> getUpdatedCategories() {
        return updatedCategories;
    }

    public void setUpdatedCategories(List<Prediction> updatedCategories) {
        this.updatedCategories = updatedCategories;
    }
}
