package com.pravalika.springapiguard.analyzer;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApiChangeAnalyzerTest {

    private ApiChangeAnalyzer analyzer;

    @Before
    public void setUp() {
        analyzer = new ApiChangeAnalyzer();
    }

    @Test
    public void noChangesShouldProduceEmptyResult() {

        ApiEndpoint endpoint =
                endpoint(
                        "GET",
                        "/users",
                        "getUsers",
                        List.of(),
                        "String"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(endpoint),
                        List.of(endpoint)
                );

        assertTrue(changes.isEmpty());
    }

    @Test
    public void addedEndpointShouldBeDetected() {

        List<ApiChange> changes =
                analyzer.compare(
                        Collections.emptyList(),
                        List.of(
                                endpoint(
                                        "GET",
                                        "/users",
                                        "getUsers",
                                        List.of(),
                                        "String"
                                )
                        )
                );

        assertEquals(1, changes.size());

        ApiChange change =
                changes.get(0);

        assertEquals(
                ApiChange.Type.ADDED,
                change.getType()
        );

        assertFalse(
                change.isBreaking()
        );

        assertEquals(
                "New endpoint",
                change.getMessage()
        );
    }

    @Test
    public void removedEndpointShouldBeBreaking() {

        ApiEndpoint endpoint =
                endpoint(
                        "GET",
                        "/users",
                        "getUsers",
                        List.of(),
                        "String"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(endpoint),
                        Collections.emptyList()
                );

        assertEquals(1, changes.size());

        ApiChange change =
                changes.get(0);

        assertEquals(
                ApiChange.Type.REMOVED,
                change.getType()
        );

        assertTrue(
                change.isBreaking()
        );
    }

    @Test
    public void pathChangeShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "GET",
                        "/users/{id}",
                        "getUser",
                        List.of(
                                parameter(
                                        "id",
                                        "Long",
                                        ApiParameter.Location.PATH,
                                        true
                                )
                        ),
                        "String"
                );

        ApiEndpoint current =
                endpoint(
                        "GET",
                        "/users/{userId}",
                        "getUser",
                        List.of(
                                parameter(
                                        "userId",
                                        "Long",
                                        ApiParameter.Location.PATH,
                                        true
                                )
                        ),
                        "String"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsType(
                        changes,
                        ApiChange.Type.PATH_CHANGED
                )
        );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Endpoint path changed"
                )
        );
    }

    @Test
    public void methodChangeShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "POST",
                        "/users",
                        "createUser",
                        List.of(),
                        "String"
                );

        ApiEndpoint current =
                endpoint(
                        "PUT",
                        "/users",
                        "createUser",
                        List.of(),
                        "String"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsType(
                        changes,
                        ApiChange.Type.METHOD_CHANGED
                )
        );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "HTTP method changed"
                )
        );
    }

    @Test
    public void requestParameterTypeChangeShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "GET",
                        "/users/{id}",
                        "getUser",
                        List.of(
                                parameter(
                                        "id",
                                        "Long",
                                        ApiParameter.Location.PATH,
                                        true
                                )
                        ),
                        "String"
                );

        ApiEndpoint current =
                endpoint(
                        "GET",
                        "/users/{id}",
                        "getUser",
                        List.of(
                                parameter(
                                        "id",
                                        "String",
                                        ApiParameter.Location.PATH,
                                        true
                                )
                        ),
                        "String"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Parameter id type changed: Long → String"
                )
        );
    }

    @Test
    public void requiredParameterAddedShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "GET",
                        "/users",
                        "getUsers",
                        List.of(),
                        "String"
                );

        ApiEndpoint current =
                endpoint(
                        "GET",
                        "/users",
                        "getUsers",
                        List.of(
                                parameter(
                                        "name",
                                        "String",
                                        ApiParameter.Location.QUERY,
                                        true
                                )
                        ),
                        "String"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Required parameter name was added"
                )
        );
    }

    @Test
    public void optionalParameterBecomingRequiredShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "GET",
                        "/users",
                        "getUsers",
                        List.of(
                                parameter(
                                        "name",
                                        "String",
                                        ApiParameter.Location.QUERY,
                                        false
                                )
                        ),
                        "String"
                );

        ApiEndpoint current =
                endpoint(
                        "GET",
                        "/users",
                        "getUsers",
                        List.of(
                                parameter(
                                        "name",
                                        "String",
                                        ApiParameter.Location.QUERY,
                                        true
                                )
                        ),
                        "String"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Parameter name became required"
                )
        );
    }

    @Test
    public void responseTypeChangeShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "GET",
                        "/users/{id}",
                        "getUser",
                        List.of(),
                        "String"
                );

        ApiEndpoint current =
                endpoint(
                        "GET",
                        "/users/{id}",
                        "getUser",
                        List.of(),
                        "UserDto"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Response type changed: String → UserDto"
                )
        );
    }

    @Test
    public void requestBodyTypeChangeShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "POST",
                        "/users",
                        "createUser",
                        List.of(
                                parameter(
                                        "request",
                                        "CreateUserRequest",
                                        ApiParameter.Location.BODY,
                                        true
                                )
                        ),
                        "User"
                );

        ApiEndpoint current =
                endpoint(
                        "POST",
                        "/users",
                        "createUser",
                        List.of(
                                parameter(
                                        "request",
                                        "CreateUserV2Request",
                                        ApiParameter.Location.BODY,
                                        true
                                )
                        ),
                        "User"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Request body type changed: " +
                                "CreateUserRequest → " +
                                "CreateUserV2Request"
                )
        );
    }

    @Test
    public void requiredRequestBodyAddedShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "POST",
                        "/users",
                        "createUser",
                        List.of(),
                        "User"
                );

        ApiEndpoint current =
                endpoint(
                        "POST",
                        "/users",
                        "createUser",
                        List.of(
                                parameter(
                                        "request",
                                        "CreateUserRequest",
                                        ApiParameter.Location.BODY,
                                        true
                                )
                        ),
                        "User"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Required request body was added"
                )
        );
    }

    @Test
    public void requestBodyTypeChangeShouldNotLookLikeEndpointRemoval() {

        ApiEndpoint baseline =
                endpoint(
                        "POST",
                        "/users/create",
                        "createUser",
                        List.of(
                                parameter(
                                        "request",
                                        "CreateUserRequest",
                                        ApiParameter.Location.BODY,
                                        true
                                )
                        ),
                        "User"
                );

        ApiEndpoint current =
                endpoint(
                        "POST",
                        "/users/create",
                        "createUser",
                        List.of(
                                parameter(
                                        "request",
                                        "CreateUserV2Request",
                                        ApiParameter.Location.BODY,
                                        true
                                )
                        ),
                        "User"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Request body type changed: " +
                                "CreateUserRequest → " +
                                "CreateUserV2Request"
                )
        );

        assertFalse(
                changes.stream()
                        .anyMatch(
                                change ->
                                        change.getType() ==
                                                ApiChange.Type.REMOVED
                        )
        );

        assertFalse(
                changes.stream()
                        .anyMatch(
                                change ->
                                        change.getType() ==
                                                ApiChange.Type.ADDED
                        )
        );
    }

    @Test
    public void requestBodyBecomingRequiredShouldBeBreaking() {

        ApiEndpoint baseline =
                endpoint(
                        "POST",
                        "/users",
                        "createUser",
                        List.of(
                                parameter(
                                        "request",
                                        "CreateUserRequest",
                                        ApiParameter.Location.BODY,
                                        false
                                )
                        ),
                        "User"
                );

        ApiEndpoint current =
                endpoint(
                        "POST",
                        "/users",
                        "createUser",
                        List.of(
                                parameter(
                                        "request",
                                        "CreateUserRequest",
                                        ApiParameter.Location.BODY,
                                        true
                                )
                        ),
                        "User"
                );

        List<ApiChange> changes =
                analyzer.compare(
                        List.of(baseline),
                        List.of(current)
                );

        assertTrue(
                containsBreakingChange(
                        changes,
                        "Request body became required"
                )
        );
    }

    private ApiEndpoint endpoint(
            String httpMethod,
            String path,
            String method,
            List<ApiParameter> parameters,
            String responseType
    ) {

        return new ApiEndpoint(
                httpMethod,
                path,
                "com.example.demo.controller.UserController",
                method,
                parameters,
                responseType
        );
    }

    private ApiParameter parameter(
            String name,
            String type,
            ApiParameter.Location location,
            boolean required
    ) {

        return new ApiParameter(
                name,
                type,
                location,
                required
        );
    }

    private boolean containsType(
            List<ApiChange> changes,
            ApiChange.Type type
    ) {

        return changes.stream()
                .anyMatch(
                        change ->
                                change.getType() == type
                );
    }

    private boolean containsBreakingChange(
            List<ApiChange> changes,
            String message
    ) {

        return changes.stream()
                .anyMatch(
                        change ->
                                change.isBreaking()
                                        &&
                                        change.getMessage()
                                                .equals(message)
                );
    }
}