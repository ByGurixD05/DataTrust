package com.datatrust.application.auth.catalog;

import java.util.UUID;

public class DatasetNotFoundException extends RuntimeException {

    private final UUID datasetId;

    public DatasetNotFoundException(UUID datasetId) {
        super("No existe ningún dataset con id " + datasetId);
        this.datasetId = datasetId;
    }

    public UUID datasetId() {
        return datasetId;
    }
}