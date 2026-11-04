package com.datatrust.api.validation;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.datatrust.api.validation.dto.ValidationReportResponse;
import com.datatrust.application.ports.CurrentUserProvider;
import com.datatrust.application.validation.ValidationService;

@RestController
@RequestMapping("/api/v1/datasets/{datasetId}/validations")
public class ValidationController {

    private final ValidationService validations;
    private final CurrentUserProvider currentUser;

    public ValidationController(ValidationService validations, CurrentUserProvider currentUser) {
        this.validations = validations;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ValidationReportResponse validate(
            @PathVariable UUID datasetId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ValidationReportResponse.from(
                validations.validate(datasetId, file.getInputStream(),
                        currentUser.currentUserId(), currentUser.currentUserRole()));
    }
}
