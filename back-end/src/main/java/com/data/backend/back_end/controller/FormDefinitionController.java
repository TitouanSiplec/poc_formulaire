package com.data.backend.back_end.controller;

import com.data.backend.back_end.model.FormDefinition;
import com.data.backend.back_end.repository.FormDefinitionRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/forms")
public class FormDefinitionController {

    @Autowired
    private FormDefinitionRepository formRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadForm(@RequestParam("formName") String formName,
                                             @RequestParam("file") MultipartFile file) {
        try {
            String jsonContent = new String(file.getBytes(), StandardCharsets.UTF_8);

            FormDefinition form = new FormDefinition();
            form.setFormName(formName);
            form.setJsonContent(jsonContent);

            formRepository.save(form);

            return ResponseEntity.ok("Formulaire '" + formName + "' enregistré avec succès.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }

    @GetMapping("/getForms")
    public List<FormDefinition> getFormsByRole() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities().isEmpty()) {
            return List.of(); // Aucun formulaire si pas d'auth
        }

        // On suppose un seul rôle
        GrantedAuthority authority = auth.getAuthorities().iterator().next();
        String role = authority.getAuthority().replace("ROLE_", ""); // Ex: "admin", "manager"

        return formRepository.findByAllowedRole(role);
    }

    @GetMapping("/getFormByName")
    public ResponseEntity<FormDefinition> getFormByName(@RequestParam String formName) {
        return formRepository.findById(formName)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/create")
    @Transactional
    public ResponseEntity<String> createForm(@RequestBody Map<String, Object> payload) {
        try {
            String formName = (String) payload.get("formName");
            String jsonContent = (String) payload.get("jsonContent");
            String allowedRole = (String) payload.get("allowedRole");

            FormDefinition form = new FormDefinition();
            form.setFormName(formName);
            form.setJsonContent(jsonContent);
            form.setAllowedRole(allowedRole);

            formRepository.save(form);

            var jsonNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(jsonContent);
            var fields = jsonNode.get("fields");

            if (fields == null || !fields.isArray()) {
                return ResponseEntity.badRequest().body("JSON invalide ou champs manquants");
            }

            StringBuilder createTableSql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            createTableSql.append(formName).append(" (id SERIAL PRIMARY KEY,");

            for (int i = 0; i < fields.size(); i++) {
                var field = fields.get(i);
                String name = field.get("name").asText();
                String type = field.get("type").asText();

                String sqlType = switch (type) {
                    case "text", "email" -> "VARCHAR(255)";
                    case "number" -> "INT";
                    default -> "VARCHAR(255)";
                };

                createTableSql.append(name).append(" ").append(sqlType);

                if (i < fields.size() - 1) {
                    createTableSql.append(", ");
                }
            }

            createTableSql.append(");");
            entityManager.createNativeQuery(createTableSql.toString()).executeUpdate();

            return ResponseEntity.ok("Formulaire créé et table générée avec succès !");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erreur: " + e.getMessage());
        }
    }

    @PostMapping("/submit")
    @Transactional
    public ResponseEntity<String> submitForm(@RequestParam String formName, @RequestBody Map<String, Object> data) {
        try {
            StringBuilder columns = new StringBuilder();
            StringBuilder values = new StringBuilder();
            List<Object> parameters = new ArrayList<>();

            for (String key : data.keySet()) {
                columns.append(key).append(", ");
                values.append("?, ");
                parameters.add(data.get(key));
            }

            if (columns.length() > 0) {
                columns.setLength(columns.length() - 2);
                values.setLength(values.length() - 2);
            }

            String sql = "INSERT INTO " + formName + " (" + columns + ") VALUES (" + values + ")";
            var query = entityManager.createNativeQuery(sql);

            for (int i = 0; i < parameters.size(); i++) {
                query.setParameter(i + 1, parameters.get(i));
            }

            query.executeUpdate();
            return ResponseEntity.ok("Réponse enregistrée avec succès.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }
}
