package com.tick.filter;

import com.tick.entity.RestBean;
import com.tick.utils.Const;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * 基于 Redis 的 IP 限流过滤器：超过阈值后拉黑一段时间并返回 429。
 * OPTIONS 预检不计数也不拦截——被拉黑时预检若返回非 2xx，浏览器只会报 CORS 错误，掩盖限流真实原因。
 * 计数与拉黑在一个 Lua 脚本里原子完成，每请求只占一次 Redis 往返。
 */
@Component
@Order(Const.ORDER_LIMIT)
public class FlowLimitFilter extends HttpFilter {

    /** 3 秒窗口内允许 50 次；超限后拉黑 30 秒。 */
    private static final int LIMIT = 50;

    private static final DefaultRedisScript<Long> FLOW_LIMIT_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[2]) == 1 then return 0 end "
                    + "local n = redis.call('INCR', KEYS[1]) "
                    + "if n == 1 then redis.call('EXPIRE', KEYS[1], 3) end "
                    + "if n > tonumber(ARGV[1]) then "
                    + "  redis.call('SET', KEYS[2], '1', 'EX', 30) return 0 "
                    + "end "
                    + "return 1",
            Long.class);

    @Resource
    StringRedisTemplate template;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        // CORS 预检不执行业务逻辑，不计数也不拦截，
        // 否则被拉黑时预检返回非 2xx，浏览器只会报 CORS 错误，掩盖限流的真实原因
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || this.tryCount(request.getRemoteAddr()))
            chain.doFilter(request, response);
        else {
            this.writeBlockMessage(response);
        }

    }

    private void writeBlockMessage(HttpServletResponse resp) throws IOException {
        resp.setStatus(429);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(RestBean.failure(429, "请求过于频繁，请稍后再试").asJsonString());
    }

    private boolean tryCount(String ip) {
        Long allowed = template.execute(FLOW_LIMIT_SCRIPT,
                List.of(Const.FLOW_LIMIT_COUNTER + ip, Const.FLOW_LIMIT_BLOCK + ip),
                String.valueOf(LIMIT));
        return Long.valueOf(1L).equals(allowed);
    }
}
