package com.ses.whodatidols.model;

import java.util.Map;

public class PushSubscriptionRequestDto {
    private String endpoint;
    private KeysDto keys;
    private String userAgent;

    public PushSubscriptionRequestDto() {
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public KeysDto getKeys() {
        return keys;
    }

    public void setKeys(KeysDto keys) {
        this.keys = keys;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public static class KeysDto {
        private String p256dh;
        private String auth;

        public KeysDto() {
        }

        public KeysDto(String p256dh, String auth) {
            this.p256dh = p256dh;
            this.auth = auth;
        }

        public String getP256dh() {
            return p256dh;
        }

        public void setP256dh(String p256dh) {
            this.p256dh = p256dh;
        }

        public String getAuth() {
            return auth;
        }

        public void setAuth(String auth) {
            this.auth = auth;
        }
    }
}
