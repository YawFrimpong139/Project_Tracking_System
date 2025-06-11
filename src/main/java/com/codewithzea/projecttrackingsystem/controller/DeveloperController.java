package com.codewithzea.projecttrackingsystem.controller;


import com.codewithzea.projecttrackingsystem.dto.DeveloperDTO;
import com.codewithzea.projecttrackingsystem.service.DeveloperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/developers")
@RequiredArgsConstructor
public class DeveloperController {

    private final DeveloperService developerService;

    @GetMapping("/{id}")
    public ResponseEntity<DeveloperDTO> getDeveloper(@PathVariable Long id) {
        return ResponseEntity.ok(developerService.getDeveloperById(id));
    }

    @GetMapping
    public ResponseEntity<Page<DeveloperDTO>> getAllDevelopers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction
    ) {
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(developerService.getAllDevelopers(pageable));
    }

    @PostMapping
    public ResponseEntity<DeveloperDTO> createDeveloper(@Valid @RequestBody DeveloperDTO dto) {
        DeveloperDTO created = developerService.createDeveloper(dto);
        return ResponseEntity.status(201).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeveloperDTO> updateDeveloper(@PathVariable Long id, @Valid @RequestBody DeveloperDTO dto) {
        return ResponseEntity.ok(developerService.updateDeveloper(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeveloper(@PathVariable Long id) {
        developerService.deleteDeveloper(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/top")
    public ResponseEntity<List<DeveloperDTO>> getTop5DevelopersByTaskCount() {
        return ResponseEntity.ok(developerService.getTop5DevelopersByTaskCount());
    }
}
