package com.ole.turapp.controller;

import com.ole.turapp.dto.NatureReserveResponse;
import com.ole.turapp.service.NatureReserveService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/nature-reserves")
public class NatureReserveController {

    private final NatureReserveService service;

    public NatureReserveController(NatureReserveService service) {
        this.service = service;
    }

    @GetMapping
    public List<NatureReserveResponse> getAll() {
        return service.getAll();
    }

    @GetMapping("/import")
    public String triggerImport() {
        return service.importNow();
    }
}
