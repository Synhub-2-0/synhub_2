package com.hub.synhub_backend.groups.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

import java.security.SecureRandom;

@Embeddable
public record GroupCode(String code) {
    private static final String ALPHANUM = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int LENGTH = 9;

    public GroupCode {
        if (code == null || code.length() != LENGTH || !code.matches("[0-9A-Z]{" + LENGTH + "}")) {
            throw new IllegalArgumentException("El código debe tener 9 caracteres alfanuméricos (0-9, A-Z)");
        }
    }

    public static GroupCode random() {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHANUM.charAt(random.nextInt(ALPHANUM.length())));
        }
        return new GroupCode(sb.toString());
    }
}
