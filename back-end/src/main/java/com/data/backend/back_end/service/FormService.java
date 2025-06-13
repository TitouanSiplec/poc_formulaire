package com.data.backend.back_end.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class FormService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public String getPrimaryKeyFromFormConfig(String tableName) {
        try {
            String sql = """
                SELECT a.attname AS column_name
                FROM pg_index i
                JOIN pg_class c ON c.oid = i.indrelid
                JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey)
                WHERE c.relname = :tableName
                AND i.indisprimary
                LIMIT 1
            """;

            return (String) entityManager.createNativeQuery(sql)
                    .setParameter("tableName", tableName)
                    .getSingleResult();

        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération de la clé primaire : " + e.getMessage());
            return null;
        }
    }

    public boolean doesPrimaryKeyValueExist(String tableName, String primaryKey, Object value) {
        try {
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + primaryKey + " = :value";
            Query query = entityManager.createNativeQuery(sql);
            query.setParameter("value", value);
            Number count = (Number) query.getSingleResult();
            return count != null && count.intValue() > 0;
        } catch (Exception e) {
            System.err.println("Erreur lors de la vérification de la clé primaire : " + e.getMessage());
            return false;
        }
    }
}
