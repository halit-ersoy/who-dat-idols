package com.ses.whodatidols.model;

import java.util.UUID;

public class Hero {
    private UUID id;
    private UUID referenceId;
    private String customSummary;
    private int sortOrder;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public String getCustomSummary() {
        return customSummary;
    }

    public void setCustomSummary(String customSummary) {
        this.customSummary = customSummary;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
