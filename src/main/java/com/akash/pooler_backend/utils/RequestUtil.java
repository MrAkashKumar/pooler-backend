package com.akash.pooler_backend.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
/*
Request util
 */
@Component
public class RequestUtil {

    private static final String[] IP_HEADERS = {
            "X-Forwarded-For","Proxy-Client-IP","WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR","HTTP_X_FORWARDED","HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP","HTTP_FORWARDED_FOR","HTTP_FORWARDED","HTTP_VIA",
            "REMOTE_ADDR"
    };

    private RequestUtil(){
        throw new IllegalArgumentException("Request utils class thrown");
    }

    public static String extractBearerToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (StringUtils.isNotBlank(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }

    public static String getClientIp(HttpServletRequest req) {
        for (String header : IP_HEADERS) {
            String ip = req.getHeader(header);
            if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        return req.getRemoteAddr();
    }

    public static String getDeviceId(HttpServletRequest req)   {
        return req.getHeader("X-Device-Id");
    }

    public static String getPlatform(HttpServletRequest req)   {
        return req.getHeader("X-Platform");
    }

    public static String getAppVersion(HttpServletRequest req) {
        return req.getHeader("X-App-Version");
    }

    public static HttpServletRequest currentRequest() {
        var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        return attrs.getRequest();
    }
}
