package com.tick.entity.vo.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TotpSetupVO {
    @Size(max = 6, message = "邮箱验证码格式不正确")
    private String emailCode;
}
