package com.pravalika.springapiguard.analyzer;

import java.util.List;
import java.util.stream.Collectors;

public final class ApiEndpoint {

    private final String httpMethod;
    private final String path;
    private final String controller;
    private final String method;
    private final List<ApiParameter> parameters;
    private final String responseType;

    public ApiEndpoint(
            String httpMethod,
            String path,
            String controller,
            String method,
            List<ApiParameter> parameters,
            String responseType
    ) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.controller = controller;
        this.method = method;
        this.parameters = parameters;
        this.responseType = responseType;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getController() {
        return controller;
    }

    public String getMethod() {
        return method;
    }

    public List<ApiParameter> getParameters() {
        return parameters;
    }

    public String getResponseType() {
        return responseType;
    }

    /**
     * Identifies the source handler without using parameter types.
     *
     * This is intentional:
     *
     * CreateUserRequest -> CreateUserV2Request
     *
     * should remain the SAME endpoint so that the analyzer reports
     * "request body type changed" rather than "endpoint removed + added".
     *
     * Parameter names and locations are included to distinguish
     * common overloaded controller methods.
     */
    public String getHandlerKey() {
        return controller + "#" + method;
    }

    public String getApiKey() {
        return httpMethod + " " + path;
    }
}