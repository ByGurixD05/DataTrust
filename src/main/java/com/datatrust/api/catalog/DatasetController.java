package com.datatrust.api.catalog;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.datatrust.api.catalog.dto.CreateDatasetRequest;
import com.datatrust.api.catalog.dto.DatasetResponse;
import com.datatrust.application.auth.catalog.DatasetService;
import com.datatrust.application.ports.CurrentUserProvider;
import com.datatrust.domain.catalog.DatasetColumn;

@RestController
@RequestMapping("/api/v1/datasets")
public class DatasetController {

    private final DatasetService datasets;
    private final CurrentUserProvider currentUser;

    public DatasetController(DatasetService datasets, CurrentUserProvider currentUser) {
        this.datasets = datasets;
        this.currentUser = currentUser;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> register(@Valid @RequestBody CreateDatasetRequest request) {
        List<DatasetColumn> columns = request.columns().stream()
                .map(c -> DatasetColumn.create(c.name(), c.position(), c.expectedType(), c.required()))
                .toList();

        // El propietario sale del usuario autenticado, nunca del cuerpo de la petición
        UUID id = datasets.register(
                request.name(),
                request.description(),
                currentUser.currentUserId(),
                request.format(),
                request.major(),
                request.minor(),
                request.patch(),
                columns);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();

        return ResponseEntity.created(location).build();
    }

    @GetMapping("/{id}")
    public DatasetResponse get(@PathVariable UUID id) {
        return DatasetResponse.from(datasets.get(
                id, currentUser.currentUserId(), currentUser.currentUserRole()));
    }

    @GetMapping
    public List<DatasetResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return datasets.list(page, size, currentUser.currentUserId(),
                        currentUser.currentUserRole()).stream()
                .map(DatasetResponse::from)
                .toList();
    }
}