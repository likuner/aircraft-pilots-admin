package com.uav.admin.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uav.admin.entity.SysOperationLog;
import com.uav.admin.mapper.SysOperationLogMapper;
import com.uav.admin.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;

/**
 * 操作日志切面：记录 controller 方法调用
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final SysOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(com.uav.admin.aspect.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        Object result;
        boolean success = true;
        String errorMsg = null;
        try {
            result = point.proceed();
        } catch (Throwable e) {
            success = false;
            errorMsg = e.getMessage();
            throw e;
        } finally {
            long cost = System.currentTimeMillis() - start;
            try {
                saveLog(point, success, errorMsg, cost);
            } catch (Exception e) {
                log.warn("保存操作日志失败: {}", e.getMessage());
            }
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint point, boolean success, String errorMsg, long cost) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Log logAnno = method.getAnnotation(Log.class);

        SysOperationLog logEntry = new SysOperationLog();
        logEntry.setUserId(SecurityUtils.getUserId());
        logEntry.setUsername(SecurityUtils.getUsername());
        logEntry.setModule(logAnno.module());
        logEntry.setOperation(logAnno.operation());
        logEntry.setMethod(point.getTarget().getClass().getName() + "." + method.getName());
        logEntry.setStatus(success ? 1 : 0);
        logEntry.setErrorMsg(errorMsg);
        logEntry.setCostTime(cost);

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntry.setRequestUrl(request.getRequestURI());
            logEntry.setRequestMethod(request.getMethod());
            logEntry.setIp(getIp(request));
        }

        // 序列化参数（跳过文件）
        try {
            Object[] args = point.getArgs();
            Object[] filtered = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                filtered[i] = (args[i] instanceof MultipartFile || args[i] instanceof HttpServletRequest)
                        ? null : args[i];
            }
            String params = objectMapper.writeValueAsString(filtered);
            logEntry.setParams(params.length() > 2000 ? params.substring(0, 2000) : params);
        } catch (Exception e) {
            logEntry.setParams("{\"unserializable\":true}");
        }
        operationLogMapper.insert(logEntry);
    }

    private String getIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        } else {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
