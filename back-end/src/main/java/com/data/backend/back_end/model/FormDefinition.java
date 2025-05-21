package com.data.backend.back_end.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Entity
@Table(name = "form_definitions")
public class FormDefinition {

    @Id
    @Column(name = "form_name", nullable = false, unique = true)
    private String formName;

    @Column(name = "json_content", columnDefinition = "TEXT")
    private String jsonContent;

    @Column(name = "allowed_role")
    private String allowedRole;

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getJsonContent() {
        return jsonContent;
    }

    public void setJsonContent(String jsonContent) {
        this.jsonContent = jsonContent;
    }

    public String getAllowedRole() {
        return allowedRole;
    }

    public void setAllowedRole(String allowedRole) {
        this.allowedRole = allowedRole;
    }
}
