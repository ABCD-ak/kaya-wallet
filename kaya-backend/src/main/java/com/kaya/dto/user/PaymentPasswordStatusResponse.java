package com.kaya.dto.user;

public record PaymentPasswordStatusResponse(
        boolean enabled
) {

    public static PaymentPasswordStatusResponse of(boolean enabled) {
        return new PaymentPasswordStatusResponse(enabled);
    }
}
