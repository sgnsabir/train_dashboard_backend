package com.banenor.audit;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@Order(1) // Ensure the aspect has high precedence in the reactive context
public class AuditLoggingAspect {

    private static final Logger auditLogger = LoggerFactory.getLogger("com.banenor.audit");

    @Pointcut("@annotation(audit)")
    public void auditAnnotatedMethods(Audit audit) {
        // Pointcut for methods annotated with @Audit
    }

    /**
     * Returns a non-null correlation id from MDC. If missing, a new one is generated and set.
     */
    private String getOrGenerateCorrelationId() {
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }
        return correlationId;
    }

    /**
     * Logs a successful audit event.
     *
     * @param methodName the method name
     * @param audit      the Audit annotation instance
     * @param finalResult the actual final result
     */
    private void logAuditMessage(String methodName, Audit audit, Object finalResult) {
        String correlationId = getOrGenerateCorrelationId();
        String logMessage = String.format(
                "AUDIT | Time: %s | CorrelationId: %s | Action: %s | Resource: %s | Method: %s | Result: %s",
                LocalDateTime.now(), correlationId, audit.action(), audit.resource(), methodName, finalResult);
        auditLogger.info(logMessage);
    }

    /**
     * Logs an audit error event.
     *
     * @param methodName the method name
     * @param audit      the Audit annotation instance
     * @param ex         the exception thrown
     */
    private void logAuditError(String methodName, Audit audit, Throwable ex) {
        String correlationId = getOrGenerateCorrelationId();
        String logMessage = String.format(
                "AUDIT ERROR | Time: %s | CorrelationId: %s | Action: %s | Resource: %s | Method: %s | Exception: %s",
                LocalDateTime.now(), correlationId, audit.action(), audit.resource(), methodName, ex.getMessage());
        auditLogger.error(logMessage, ex);
    }

    /**
     * Around advice for methods annotated with @Audit.
     * This advice intercepts method calls and attaches reactive callbacks for Mono and Flux results.
     *
     * @param joinPoint the join point
     * @param audit     the Audit annotation instance
     * @return the (possibly wrapped) result of the method call
     * @throws Throwable if the intercepted method throws any exception
     */
    @Around("auditAnnotatedMethods(audit)")
    public Object aroundAudit(ProceedingJoinPoint joinPoint, Audit audit) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        try {
            Object result = joinPoint.proceed();
            if (result instanceof Mono) {
                return ((Mono<?>) result)
                        .doOnSuccess(finalResult -> logAuditMessage(methodName, audit, finalResult))
                        .doOnError(ex -> logAuditError(methodName, audit, ex));
            } else if (result instanceof Flux) {
                return ((Flux<?>) result)
                        .collectList()
                        .doOnSuccess(finalResults -> logAuditMessage(methodName, audit, finalResults))
                        .flatMapMany(list -> Flux.fromIterable(list));
            } else {
                logAuditMessage(methodName, audit, result);
                return result;
            }
        } catch (Throwable ex) {
            logAuditError(methodName, audit, ex);
            throw ex;
        }
    }
}
