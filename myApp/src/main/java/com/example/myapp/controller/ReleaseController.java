package com.example.myapp.controller;

import com.example.myapp.dto.ReleaseDTO;
import com.example.myapp.service.ReleaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/releases")
public class ReleaseController {
    private final ReleaseService releaseService;

    public ReleaseController(ReleaseService releaseService) {
        this.releaseService = releaseService;
    }

    @PostMapping
    public ResponseEntity<ReleaseDTO> createRelease(@Valid @RequestBody ReleaseDTO releaseDTO) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(releaseService.createRelease(releaseDTO));
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<ReleaseDTO>> getReleasesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(releaseService.getReleasesByProject(projectId));
    }

    @GetMapping
    public ResponseEntity<List<ReleaseDTO>> getAllReleases() {
        return ResponseEntity.ok(releaseService.getAllReleases());
    }

    /*@PostMapping("/{id}/branch")
    public ResponseEntity<Void> createReleaseBranch(
            @PathVariable Long id,
            @RequestParam String sourceBranch) {
        releaseService.createReleaseBranch(id, sourceBranch);
        return ResponseEntity.ok().build();
    }
    */
}
