package co.edu.unbosque.marketplace.reportes.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.unbosque.marketplace.reportes.dto.DashboardResponse;
import co.edu.unbosque.marketplace.reportes.service.DashboardService;

// Dashboard del administrador
@RestController
@RequestMapping("/admin")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestHeader(value = "Authorization", required = false) String autorizacion) {
        return dashboardService.consultar(autorizacion);
    }
}