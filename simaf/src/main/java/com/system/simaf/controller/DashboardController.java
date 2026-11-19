package com.system.simaf.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.system.simaf.dto.DTODashboard;
import com.system.simaf.service.DashboardService;

@RestController 
@RequestMapping("/avaliacoes/{avaliacaoId}/dashboard")
public class DashboardController {
    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping 
    public DTODashboard dashboard(@PathVariable Integer avaliacaoId) {
        return service.gerar(avaliacaoId);
    }
}
