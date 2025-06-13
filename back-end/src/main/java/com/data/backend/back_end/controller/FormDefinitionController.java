package com.data.backend.back_end.controller;

import com.data.backend.back_end.model.FormDefinition;
import com.data.backend.back_end.repository.FormDefinitionRepository;
import com.data.backend.back_end.service.FormService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    private FormService formService;

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
    public List<FormDefinition> getForms() {
        return formRepository.findAll();
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

            var objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var jsonNode = objectMapper.readTree(jsonContent);
            var fields = jsonNode.get("fields");
            var primaryKeyNode = jsonNode.get("primaryKey");

            if (fields == null || !fields.isArray()) {
                return ResponseEntity.badRequest().body("JSON invalide ou champs manquants");
            }

            // Récupérer les colonnes de clé primaire
            List<String> primaryKeys = new ArrayList<>();
            if (primaryKeyNode != null && primaryKeyNode.isArray()) {
                for (var pk : primaryKeyNode) {
                    primaryKeys.add(pk.asText());
                }
            }

            if (primaryKeys.isEmpty()) {
                return ResponseEntity.badRequest().body("Aucune clé primaire spécifiée.");
            }

            // Création de la requête SQL
            StringBuilder createTableSql = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
            createTableSql.append(formName).append(" (");

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

            // Ajouter la clause PRIMARY KEY
            createTableSql.append(", PRIMARY KEY (").append(String.join(", ", primaryKeys)).append(")");

            createTableSql.append(");");

            // Afficher la requête pour debug
            System.out.println("Requête SQL : " + createTableSql);

            // Exécuter la requête
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
            // ✅ Récupération de la clé primaire depuis la table
            String primaryKey = formService.getPrimaryKeyFromFormConfig(formName);
            if (primaryKey == null || !data.containsKey(primaryKey)) {
                System.out.println("Clé primaire manquante ou non définie.");
                return ResponseEntity.badRequest().body("Clé primaire manquante ou non définie.");
            }

            Object primaryKeyValue = data.get(primaryKey);

            // ✅ Vérifie si la valeur de clé primaire existe déjà
            if (formService.doesPrimaryKeyValueExist(formName, primaryKey, primaryKeyValue)) {
                System.out.println("CLE_PRIMAIRE_EXISTANTE");
                return ResponseEntity.ok("CLE_PRIMAIRE_EXISTANTE");
            }

            // ✅ Construction de la requête SQL dynamique
            StringBuilder columns = new StringBuilder();
            StringBuilder values = new StringBuilder();
            List<Object> parameters = new ArrayList<>();

            for (String key : data.keySet()) {
                columns.append(key).append(", ");
                values.append("?, ");
                parameters.add(data.get(key));
            }

            // Retire la virgule et l’espace de fin
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
            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : e.toString();
            return ResponseEntity.internalServerError().body("Erreur : " + message);
        }
    }
    


    @PostMapping("/forceSubmit")
    @Transactional
    public ResponseEntity<String> forceSubmitForm( @RequestParam String formName, @RequestParam String primaryKeyName, @RequestParam String primaryKeyValue, @RequestBody Map<String, Object> data) {
        System.out.println("🔥 REQUETE RECUE /forceSubmit");
        System.out.println(formName);
        System.out.println(primaryKeyName);
        System.out.println(primaryKeyValue);
        System.out.println(data);
        System.out.println("debut forceSubmit");
        try {
            if (primaryKeyName == null || primaryKeyName.isEmpty() || primaryKeyValue == null || primaryKeyValue.isEmpty()) {
                return ResponseEntity.badRequest().body("Clé primaire ou valeur manquante.");
            }
    
            if (data == null || data.isEmpty()) {
                return ResponseEntity.badRequest().body("Aucune donnée à mettre à jour.");
            }
    
            // Construction de la clause SET
            StringBuilder setClause = new StringBuilder();
            List<Object> parameters = new ArrayList<>();
    
            for (String key : data.keySet()) {
                setClause.append(key).append(" = ?, ");
                parameters.add(data.get(key));
            }
    
            if (setClause.length() > 0) {
                setClause.setLength(setClause.length() - 2); // supprimer la dernière virgule
            } else {
                return ResponseEntity.badRequest().body("Aucune colonne à mettre à jour.");
            }
            
            String sql = "UPDATE " + formName + " SET " + setClause + " WHERE " + primaryKeyName + " = ?";
            System.out.println(sql);
            var query = entityManager.createNativeQuery(sql);
    
            int i = 1;
            for (Object param : parameters) {
                query.setParameter(i++, param);
            }
            query.setParameter(i, primaryKeyValue);
    
            int rows = query.executeUpdate();
    
            if (rows > 0) {
                return ResponseEntity.ok("Ligne mise à jour avec succès.");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Aucune ligne trouvée avec cette clé primaire.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("Erreur : " + e.getMessage());
        }
    }
    



    @GetMapping("/getFormsByRole")
    public List<FormDefinition> getFormsByRole(@RequestParam String role) {
        return formRepository.findAll().stream()
                .filter(form -> role.equals(form.getAllowedRole()))
                .toList();
    }

}
