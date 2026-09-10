package com.devopsclassroom.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoUsuario {
    ADMIN,
    USER,
    ALUNO,
    PROFESSOR,
    AI;

    @JsonCreator
    public static TipoUsuario fromValue(String value) {
        if (value == null) {
            return null;
        }
        return TipoUsuario.valueOf(value.trim().toUpperCase());
    }
}