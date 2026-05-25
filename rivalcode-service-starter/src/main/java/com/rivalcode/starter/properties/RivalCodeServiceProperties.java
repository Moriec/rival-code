package com.rivalcode.starter.properties;

import com.rivalcode.starter.tracing.TraceConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rivalcode.service")
public class RivalCodeServiceProperties {

    private String name;
    private String version = "unknown";
    private String traceHeaderName = TraceConstants.DEFAULT_TRACE_HEADER;
    private boolean traceIdEnabled = true;
    private boolean errorHandlingEnabled = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTraceHeaderName() {
        return traceHeaderName;
    }

    public void setTraceHeaderName(String traceHeaderName) {
        this.traceHeaderName = traceHeaderName;
    }

    public boolean isTraceIdEnabled() {
        return traceIdEnabled;
    }

    public void setTraceIdEnabled(boolean traceIdEnabled) {
        this.traceIdEnabled = traceIdEnabled;
    }

    public boolean isErrorHandlingEnabled() {
        return errorHandlingEnabled;
    }

    public void setErrorHandlingEnabled(boolean errorHandlingEnabled) {
        this.errorHandlingEnabled = errorHandlingEnabled;
    }
}
