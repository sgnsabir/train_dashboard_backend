package com.banenor.audit;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.TestPropertySource;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

@SpringBootTest(classes = {
        AuditLoggingAspectTest.TestConfig.class,
        AuditLoggingAspect.class,
        AuditLoggingAspectTest.DummyAuditService.class,
        AuditLoggingAspectTest.DummyAuditServiceWithException.class
})
@TestPropertySource(locations = "classpath:test.properties")
class AuditLoggingAspectTest {

    private ListAppender<ILoggingEvent> listAppender;
    private ch.qos.logback.classic.Logger auditLogger;

    @Autowired
    private DummyAuditService dummyAuditService;

    @Autowired
    private DummyAuditServiceWithException dummyAuditServiceWithException;

    @BeforeEach
    void setup() {
        // Set up a ListAppender to capture log events
        LoggerContext context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
        auditLogger = context.getLogger("com.banenor.audit");
        listAppender = new ListAppender<>();
        listAppender.setContext(context);
        listAppender.start();
        auditLogger.addAppender(listAppender);
    }

    @AfterEach
    void tearDown() {
        auditLogger.detachAppender(listAppender);
        MDC.clear();
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfig {

        @Bean
        public DummyAuditService dummyAuditService() {
            return new DummyAuditService();
        }

        @Bean
        public DummyAuditServiceWithException dummyAuditServiceWithException() {
            return new DummyAuditServiceWithException();
        }
    }

    public static class DummyAuditService {
        @Audit(action = "TestAction", resource = "TestResource")
        public String doSomething(String input) {
            return "Processed " + input;
        }
    }

    public static class DummyAuditServiceWithException {
        @Audit(action = "TestExceptionAction", resource = "TestExceptionResource")
        public String doSomethingRisky(String input) {
            throw new RuntimeException("Simulated exception");
        }
    }

    @Test
    void testAuditLoggingAspect_SuccessfulExecution() {
        MDC.put("correlationId", "corr-1234");

        String result = dummyAuditService.doSomething("testInput");
        assertThat(result).isEqualTo("Processed testInput");

        boolean found = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("AUDIT |")
                        && event.getFormattedMessage().contains("TestAction")
                        && event.getFormattedMessage().contains("TestResource")
                        && event.getFormattedMessage().contains("doSomething")
                        && event.getFormattedMessage().contains("corr-1234"));
        assertThat(found).isTrue();
    }

    @Test
    void testAuditLoggingAspect_ExceptionExecution() {
        MDC.put("correlationId", "corr-5678");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> dummyAuditServiceWithException.doSomethingRisky("failInput"));
        assertThat(thrown.getMessage()).isEqualTo("Simulated exception");

        boolean errorFound = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("AUDIT ERROR |")
                        && event.getFormattedMessage().contains("TestExceptionAction")
                        && event.getFormattedMessage().contains("TestExceptionResource")
                        && event.getFormattedMessage().contains("doSomethingRisky")
                        && event.getFormattedMessage().contains("Simulated exception")
                        && event.getFormattedMessage().contains("corr-5678"));
        assertThat(errorFound).isTrue();
    }

    @Test
    void testAuditLoggingAspect_CorrelationIdNotProvided() {
        MDC.clear();

        String result = dummyAuditService.doSomething("noCorr");
        assertThat(result).isEqualTo("Processed noCorr");

        boolean found = listAppender.list.stream()
                .anyMatch(event -> event.getFormattedMessage().contains("AUDIT |")
                        && event.getFormattedMessage().contains("TestAction")
                        && event.getFormattedMessage().contains("TestResource")
                        && event.getFormattedMessage().contains("doSomething"));
        assertThat(found).isTrue();
    }
}
