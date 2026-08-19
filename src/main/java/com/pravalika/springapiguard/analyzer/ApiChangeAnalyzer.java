package com.pravalika.springapiguard.analyzer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApiChangeAnalyzer {

    public List<ApiChange> compare(
            List<ApiEndpoint> baseline,
            List<ApiEndpoint> current
    ) {

        List<ApiChange> changes =
                new ArrayList<>();

        Map<String, ApiEndpoint> baselineByHandler =
                indexByHandler(baseline);

        Map<String, ApiEndpoint> currentByHandler =
                indexByHandler(current);

        for (ApiEndpoint currentEndpoint : current) {

            ApiEndpoint previous =
                    baselineByHandler.get(
                            currentEndpoint.getHandlerKey()
                    );

            if (previous == null) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.ADDED,
                                null,
                                currentEndpoint,
                                "New endpoint",
                                false
                        )
                );

                continue;
            }

            if (!previous.getHttpMethod()
                    .equals(currentEndpoint.getHttpMethod())) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.METHOD_CHANGED,
                                previous,
                                currentEndpoint,
                                "HTTP method changed",
                                true
                        )
                );
            }

            if (!previous.getPath()
                    .equals(currentEndpoint.getPath())) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.PATH_CHANGED,
                                previous,
                                currentEndpoint,
                                "Endpoint path changed",
                                true
                        )
                );
            }

            compareParameters(
                    previous,
                    currentEndpoint,
                    changes
            );

            if (!previous.getResponseType()
                    .equals(currentEndpoint.getResponseType())) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.RESPONSE_CHANGED,
                                previous,
                                currentEndpoint,
                                "Response type changed: " +
                                        previous.getResponseType() +
                                        " → " +
                                        currentEndpoint.getResponseType(),
                                true
                        )
                );
            }
        }

        Set<String> currentHandlers =
                new HashSet<>(
                        currentByHandler.keySet()
                );

        for (ApiEndpoint baselineEndpoint :
                baseline) {

            if (!currentHandlers.contains(
                    baselineEndpoint.getHandlerKey()
            )) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.REMOVED,
                                baselineEndpoint,
                                null,
                                "Endpoint removed",
                                true
                        )
                );
            }
        }

        changes.sort(
                Comparator
                        .comparing(
                                (ApiChange c) ->
                                        !c.isBreaking()
                        )
                        .thenComparing(
                                c -> c.getType().ordinal()
                        )
                        .thenComparing(
                                c -> {
                                    ApiEndpoint endpoint =
                                            c.getAfter() != null
                                                    ? c.getAfter()
                                                    : c.getBefore();

                                    return endpoint == null
                                            ? ""
                                            : endpoint.getPath();
                                }
                        )
        );

        return changes;
    }

    private Map<String, ApiEndpoint> indexByHandler(
            List<ApiEndpoint> endpoints
    ) {

        Map<String, ApiEndpoint> result =
                new HashMap<>();

        for (ApiEndpoint endpoint : endpoints) {

            result.put(
                    endpoint.getHandlerKey(),
                    endpoint
            );
        }

        return result;
    }

    private void compareParameters(
            ApiEndpoint before,
            ApiEndpoint after,
            List<ApiChange> changes
    ) {

        Map<String, ApiParameter> beforeParams =
                indexParameters(before);

        Map<String, ApiParameter> afterParams =
                indexParameters(after);

        /*
         * Existing parameters.
         */
        for (Map.Entry<String, ApiParameter> entry :
                beforeParams.entrySet()) {

            ApiParameter oldParameter =
                    entry.getValue();

            ApiParameter newParameter =
                    afterParams.get(
                            entry.getKey()
                    );

            if (newParameter == null) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.REQUEST_CHANGED,
                                before,
                                after,
                                removedParameterMessage(
                                        oldParameter
                                ),
                                false
                        )
                );

                continue;
            }

            if (!oldParameter.getType()
                    .equals(newParameter.getType())) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.REQUEST_CHANGED,
                                before,
                                after,
                                changedParameterTypeMessage(
                                        oldParameter,
                                        newParameter
                                ),
                                true
                        )
                );
            }

            /*
             * Optional → required is breaking.
             */
            if (!oldParameter.isRequired()
                    && newParameter.isRequired()) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.REQUEST_CHANGED,
                                before,
                                after,
                                becameRequiredMessage(
                                        newParameter
                                ),
                                true
                        )
                );
            }
        }

        /*
         * Newly added parameters.
         */
        for (Map.Entry<String, ApiParameter> entry :
                afterParams.entrySet()) {

            if (beforeParams.containsKey(
                    entry.getKey()
            )) {
                continue;
            }

            ApiParameter newParameter =
                    entry.getValue();

            if (newParameter.isRequired()) {

                changes.add(
                        new ApiChange(
                                ApiChange.Type.REQUEST_CHANGED,
                                before,
                                after,
                                addedRequiredParameterMessage(
                                        newParameter
                                ),
                                true
                        )
                );
            }
        }
    }

    private Map<String, ApiParameter> indexParameters(
            ApiEndpoint endpoint
    ) {

        Map<String, ApiParameter> result =
                new HashMap<>();

        for (ApiParameter parameter :
                endpoint.getParameters()) {

            result.put(
                    parameter.key(),
                    parameter
            );
        }

        return result;
    }

    private String changedParameterTypeMessage(
            ApiParameter before,
            ApiParameter after
    ) {

        if (after.getLocation() ==
                ApiParameter.Location.BODY) {

            return "Request body type changed: " +
                    before.getType() +
                    " → " +
                    after.getType();
        }

        return "Parameter " +
                before.getName() +
                " type changed: " +
                before.getType() +
                " → " +
                after.getType();
    }

    private String removedParameterMessage(
            ApiParameter parameter
    ) {

        if (parameter.getLocation() ==
                ApiParameter.Location.BODY) {

            return "Request body was removed";
        }

        return "Parameter " +
                parameter.getName() +
                " was removed";
    }

    private String becameRequiredMessage(
            ApiParameter parameter
    ) {

        if (parameter.getLocation() ==
                ApiParameter.Location.BODY) {

            return "Request body became required";
        }

        return "Parameter " +
                parameter.getName() +
                " became required";
    }

    private String addedRequiredParameterMessage(
            ApiParameter parameter
    ) {

        if (parameter.getLocation() ==
                ApiParameter.Location.BODY) {

            return "Required request body was added";
        }

        return "Required parameter " +
                parameter.getName() +
                " was added";
    }
}