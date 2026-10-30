package com.ole.turapp.controller;

import com.ole.turapp.config.AuthUtils;
import com.ole.turapp.dto.GarminImportResponse;
import com.ole.turapp.service.GarminService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GarminController {

    private final GarminService garminService;

    public GarminController(GarminService garminService) {
        this.garminService = garminService;
    }

    @PostMapping("/users/{userId}/garmin/activities/{activityId}/import")
    public ResponseEntity<GarminImportResponse> importActivity(
            @PathVariable Long userId,
            @PathVariable long activityId) {
        AuthUtils.requireOwner(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(garminService.importActivity(userId, activityId));
    }
}
