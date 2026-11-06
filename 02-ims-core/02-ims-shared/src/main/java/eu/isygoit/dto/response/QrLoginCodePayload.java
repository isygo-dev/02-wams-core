package eu.isygoit.dto.response;

import eu.isygoit.exception.BadArgumentException;

public record QrLoginCodePayload(String type, String challengeId) {

    public static final String TYPE = "isygo-qr-login";

    public static QrLoginCodePayload forChallenge(String challengeId) {
        if (challengeId == null || !challengeId.matches("[A-Za-z0-9_-]{43}")) {
            throw new BadArgumentException("QR challenge identifiers must be URL-safe random values");
        }
        return new QrLoginCodePayload(TYPE, challengeId);
    }

    public String toQrContent() {
        return "{\"type\":\"" + type + "\",\"challengeId\":\"" + challengeId + "\"}";
    }
}
