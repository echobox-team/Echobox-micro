package com.EchoBox.controller;

import com.EchoBox.model.Status;
import com.EchoBox.repository.StatusRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/statuses")
@Tag(name = "Status (ms-company)", description = "API endpoints for status management")
public class StatusController {

    private final StatusRepository statusRepository;

    public StatusController(StatusRepository statusRepository) {
        this.statusRepository = statusRepository;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Creates a new status")
    public ResponseEntity<Status> save(@Valid @RequestBody Status status) {
        Status savedStatus = statusRepository.save(status);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedStatus);
    }

    @GetMapping
    @Operation(summary = "Gets a list of statuses")
    public List<Status> findAll() {
        return statusRepository.findAll();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deletes a status")
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        if (!statusRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found");
        }
        statusRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Updates a status")
    public ResponseEntity<Status> update(@PathVariable("id") Integer id, @RequestBody Status status) {
        if (!statusRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found");
        }
        status.setId(id);
        Status updatedStatus = statusRepository.save(status);
        return ResponseEntity.ok(updatedStatus);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Gets a status by ID")
    public ResponseEntity<Status> findById(@PathVariable("id") Integer id) {
        return statusRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Status not found"));
    }
}
