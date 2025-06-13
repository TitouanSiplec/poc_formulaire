package com.data.backend.back_end.controller;

import com.data.backend.back_end.service.GoogleSheetsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sheet")
public class SheetsController {

    @PostMapping("/submit")
    public ResponseEntity<?> submitForm(
            @RequestParam String formName,
            @RequestBody Map<String, Object> formData
    ) {
        try {
            List<Object> row = formData.values().stream().toList();
            GoogleSheetsService.addRowToSheet(formName, row); // 👈 tu passes formName ici
            return ResponseEntity.ok("Formulaire inséré dans Google Sheets.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur : " + e.getMessage());
        }
    }

    @PostMapping("/create-sheet")
    public ResponseEntity<String> createSheet(@RequestParam String sheetName,
                                            @RequestBody List<String> headers) {
        try {
            GoogleSheetsService.createSheetWithHeaders(sheetName, headers);
            return ResponseEntity.ok("Feuille créée avec succès : " + sheetName);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Erreur lors de la création de la feuille : " + e.getMessage());
        }
    }
}
