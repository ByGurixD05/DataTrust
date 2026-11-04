package com.datatrust.application.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.datatrust.application.auth.catalog.DatasetService;
import com.datatrust.domain.catalog.DatasetSchemaSnapshot;
import com.datatrust.domain.catalog.DatasetWithCurrentVersion;
import com.datatrust.domain.enums.UserRole;

@Service
public class ValidationService {

    private final DatasetService datasets;
    private final RowReaderFactory readers;
    private final ContentValidator validator;

    public ValidationService(DatasetService datasets, RowReaderFactory readers,
                             ContentValidator validator) {
        this.datasets = datasets;
        this.readers = readers;
        this.validator = validator;
    }

    public ValidationReport validate(
            UUID datasetId, InputStream content, UUID requesterId, UserRole requesterRole)
            throws IOException {
        DatasetWithCurrentVersion catalogEntry = datasets.get(datasetId, requesterId, requesterRole);
        DatasetSchemaSnapshot snapshot = catalogEntry.currentVersion().schemaSnapshot();
        try (RowReader reader = readers.open(snapshot.format(), content)) {
            return validator.validate(reader, snapshot.columns());
        }
    }
}
