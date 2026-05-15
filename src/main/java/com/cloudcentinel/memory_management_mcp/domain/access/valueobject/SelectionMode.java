package com.cloudcentinel.memory_management_mcp.domain.access.valueobject;

public enum SelectionMode {
    /** El usuario ve toda la batería del proyecto más sus skills privados. */
    ADDITIVE,
    /** El usuario ve solo los skills que seleccionó explícitamente de la batería. */
    RESTRICTIVE
}
