package com.mrll.javelin.api.smarttools.publisher.model;

import com.google.common.base.MoreObjects;
import com.mrll.javelin.api.smarttools.model.delegates.FolderSuggestion;
import com.mrll.javelin.api.smarttools.model.delegates.Prediction;

import java.util.List;

public class CategorizationEvent {
    private String projectId;
    private String metadataId;
    private String parentId;
    private List<Prediction> predictions;
    private List<FolderSuggestion> folderSuggestions;

    public String getProjectId() {
        return projectId;
    }

    public CategorizationEvent setProjectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getMetadataId() {
        return metadataId;
    }

    public CategorizationEvent setMetadataId(String metadataId) {
        this.metadataId = metadataId;
        return this;
    }

    public String getParentId() {
        return parentId;
    }

    public CategorizationEvent setParentId(String parentId) {
        this.parentId = parentId;
        return this;
    }

    public List<Prediction> getPredictions() {
        return predictions;
    }

    public CategorizationEvent setPredictions(List<Prediction> predictions) {
        this.predictions = predictions;
        return this;
    }

    public List<FolderSuggestion> getFolderSuggestions() {
        return folderSuggestions;
    }

    public CategorizationEvent setFolderSuggestions(List<FolderSuggestion> folderSuggestions) {
        this.folderSuggestions = folderSuggestions;
        return this;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("projectId", projectId)
                .add("metadataId", metadataId)
                .add("predictions", predictions)
                .add("parentId", parentId)
                .add("folderSuggestions", folderSuggestions)
                .toString();
    }
}
