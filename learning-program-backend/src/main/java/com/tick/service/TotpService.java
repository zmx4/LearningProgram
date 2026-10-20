package com.tick.service;

public interface TotpService {
    public String createSecret();
    public String getQrCodeUrl(String account, String issuer, String secret);
    public boolean verifyCode(String secret, int code);
}
