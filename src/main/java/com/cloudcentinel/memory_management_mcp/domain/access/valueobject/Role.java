package com.cloudcentinel.memory_management_mcp.domain.access.valueobject;

public enum Role {
    /** Gestiona el catálogo global de skills y puede crear proyectos. */
    GLOBAL_ADMIN,
    /** Gestiona la batería de skills del proyecto. */
    PROJECT_ADMIN,
    /** Configura sus preferencias personales de skills. */
    USER
}
