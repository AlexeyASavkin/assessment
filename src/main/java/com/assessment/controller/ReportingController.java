package com.assessment.controller;

import com.assessment.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/employee")
public class ReportingController {

    private final ReportService reportService;

    public ReportingController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/sessions/{sessionId}/report")
    public ResponseEntity<Map<String, Object>> getReport(@PathVariable UUID sessionId) {
        try {
            Map<String, Object> report = reportService.generateReport(sessionId);
            return ResponseEntity.ok(report);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
