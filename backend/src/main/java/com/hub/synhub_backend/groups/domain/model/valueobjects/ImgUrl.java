package com.hub.synhub_backend.groups.domain.model.valueobjects;

import jakarta.persistence.Embeddable;

@Embeddable
public record ImgUrl(String imgUrl) {
    public ImgUrl {
        if (imgUrl == null || imgUrl.isBlank()) {
            imgUrl = "";
        }
    }
}
