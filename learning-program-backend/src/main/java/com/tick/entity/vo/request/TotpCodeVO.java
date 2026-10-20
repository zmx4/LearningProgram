package com.tick.entity.vo.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TotpCodeVO {
    @NotNull(message = "验证码不能为空")
    @Min(value = 0, message = "验证码格式不正确")
    @Max(value = 999999, message = "验证码格式不正确")
    private Integer code;
}
