package com.example.security.domain;

public enum Role {
    ADMIN("Administrator", "ROLE_ADMIN"),
    EMPLOYEE("Employee", "ROLE_EMPLOYEE");

    private final String displayName;
    private final String springSecurityRole;

    Role(String displayName, String springSecurityRole) {
        this.displayName = displayName;
        this.springSecurityRole = springSecurityRole;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSpringSecurityRole() {
        return springSecurityRole;
    }
}
