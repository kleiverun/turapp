package com.ole.turapp.controller;

import com.ole.turapp.dto.NationalParkResponse;
import com.ole.turapp.service.NationalParkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/national-parks")
public class NationalParkController {

    private final NationalParkService service;

    public NationalParkController(NationalParkService service) {
        this.service = service;
    }

    @GetMapping
    public List<NationalParkResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/import")
    public String triggerImport() {
        return service.importNow();
    }
}
