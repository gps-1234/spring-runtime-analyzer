package com.pravalika.springapiguard.analyzer;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.PsiJavaFile;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SpringApiScanner {

    private static final Pattern NAMED_PATH_PATTERN =
            Pattern.compile(
                    "(?:value|path)\\s*=\\s*\"([^\"]*)\""
            );

    private static final Pattern SHORTHAND_PATH_PATTERN =
            Pattern.compile(
                    "\\(\\s*\"([^\"]*)\"\\s*\\)"
            );

    private static final Pattern ANNOTATION_NAME_PATTERN =
            Pattern.compile(
                    "(?:value|name)\\s*=\\s*\"([^\"]*)\""
            );

    private static final Pattern BOOLEAN_PATTERN =
            Pattern.compile(
                    "required\\s*=\\s*(true|false)"
            );

    private final Project project;

    public SpringApiScanner(Project project) {
        this.project = project;
    }

    public List<ApiEndpoint> scanProject() {

        GlobalSearchScope scope =
                GlobalSearchScope.projectScope(project);

        Collection<VirtualFile> javaFiles =
                FileTypeIndex.getFiles(
                        JavaFileType.INSTANCE,
                        scope
                );

        return scanFiles(javaFiles);
    }

    public List<ApiEndpoint> scanRoot(
            VirtualFile root
    ) {

        List<VirtualFile> javaFiles =
                new ArrayList<>();

        VfsUtilCore.iterateChildrenRecursively(
                root,
                null,
                file -> {
                    if (!file.isDirectory()
                            && "java".equalsIgnoreCase(file.getExtension())) {
                        javaFiles.add(file);
                    }

                    return true;
                }
        );

        return scanFiles(javaFiles);
    }

    private List<ApiEndpoint> scanFiles(
            Collection<VirtualFile> javaFiles
    ) {

        PsiManager psiManager =
                PsiManager.getInstance(project);

        Map<String, ApiEndpoint> endpoints =
                new LinkedHashMap<>();

        for (VirtualFile virtualFile : javaFiles) {

            PsiJavaFile psiFile =
                    findJavaFile(
                            psiManager,
                            virtualFile
                    );

            if (psiFile == null) {
                continue;
            }

            for (PsiClass psiClass : psiFile.getClasses()) {

                if (!isController(psiClass)) {
                    continue;
                }

                String classPath =
                        getRequestMappingPath(psiClass);

                for (PsiMethod method : psiClass.getMethods()) {

                    List<Mapping> mappings =
                            getRequestMappings(method);

                    for (Mapping mapping : mappings) {

                        String fullPath =
                                combinePaths(
                                        classPath,
                                        mapping.path
                                );

                        String controller =
                                psiClass.getQualifiedName();

                        if (controller == null) {
                            controller =
                                    psiClass.getName();
                        }

                        if (controller == null) {
                            controller =
                                    "UnknownController";
                        }

                        String methodName =
                                method.getName();

                        List<ApiParameter> parameters =
                                extractParameters(method);

                        String responseType =
                                method.getReturnType() != null
                                        ? method.getReturnType()
                                          .getPresentableText()
                                        : "void";

                        ApiEndpoint endpoint =
                                new ApiEndpoint(
                                        mapping.httpMethod,
                                        fullPath,
                                        controller,
                                        methodName,
                                        parameters,
                                        responseType
                                );

                        String uniqueKey =
                                endpoint.getHandlerKey();

                        endpoints.put(
                                uniqueKey,
                                endpoint
                        );
                    }
                }
            }
        }

        List<ApiEndpoint> result =
                new ArrayList<>(endpoints.values());

        result.sort(
                Comparator
                        .comparing(ApiEndpoint::getPath)
                        .thenComparing(ApiEndpoint::getHttpMethod)
                        .thenComparing(ApiEndpoint::getController)
                        .thenComparing(ApiEndpoint::getMethod)
        );

        return result;
    }

    private PsiJavaFile findJavaFile(
            PsiManager psiManager,
            VirtualFile virtualFile
    ) {

        var psiFile =
                psiManager.findFile(virtualFile);

        if (psiFile instanceof PsiJavaFile) {
            return (PsiJavaFile) psiFile;
        }

        return null;
    }

    private boolean isController(
            PsiClass psiClass
    ) {

        return hasAnnotation(
                psiClass,
                "RestController"
        ) || hasAnnotation(
                psiClass,
                "Controller"
        );
    }

    private boolean hasAnnotation(
            PsiClass psiClass,
            String annotationName
    ) {

        for (PsiAnnotation annotation :
                psiClass.getAnnotations()) {

            String qualifiedName =
                    annotation.getQualifiedName();

            if (qualifiedName == null) {
                continue;
            }

            String simpleName =
                    qualifiedName.substring(
                            qualifiedName.lastIndexOf('.') + 1
                    );

            if (annotationName.equals(simpleName)) {
                return true;
            }
        }

        return false;
    }

    private String getRequestMappingPath(
            PsiClass psiClass
    ) {

        for (PsiAnnotation annotation :
                psiClass.getAnnotations()) {

            if (isAnnotation(
                    annotation,
                    "RequestMapping"
            )) {
                return extractPath(annotation);
            }
        }

        return "";
    }

    private List<Mapping> getRequestMappings(
            PsiMethod method
    ) {

        List<Mapping> result =
                new ArrayList<>();

        for (PsiAnnotation annotation :
                method.getAnnotations()) {

            String name =
                    getSimpleAnnotationName(annotation);

            if (name == null) {
                continue;
            }

            switch (name) {

                case "GetMapping":
                    result.add(
                            new Mapping(
                                    "GET",
                                    extractPath(annotation)
                            )
                    );
                    break;

                case "PostMapping":
                    result.add(
                            new Mapping(
                                    "POST",
                                    extractPath(annotation)
                            )
                    );
                    break;

                case "PutMapping":
                    result.add(
                            new Mapping(
                                    "PUT",
                                    extractPath(annotation)
                            )
                    );
                    break;

                case "DeleteMapping":
                    result.add(
                            new Mapping(
                                    "DELETE",
                                    extractPath(annotation)
                            )
                    );
                    break;

                case "PatchMapping":
                    result.add(
                            new Mapping(
                                    "PATCH",
                                    extractPath(annotation)
                            )
                    );
                    break;

                case "RequestMapping":
                    result.add(
                            new Mapping(
                                    extractHttpMethod(annotation),
                                    extractPath(annotation)
                            )
                    );
                    break;

                default:
                    break;
            }
        }

        return result;
    }

    private List<ApiParameter> extractParameters(
            PsiMethod method
    ) {

        List<ApiParameter> result =
                new ArrayList<>();

        for (PsiParameter parameter :
                method.getParameterList().getParameters()) {

            PsiAnnotation annotation =
                    findRequestParameterAnnotation(
                            parameter
                    );

            if (annotation == null) {
                continue;
            }

            String annotationName =
                    getSimpleAnnotationName(annotation);

            if (annotationName == null) {
                continue;
            }

            switch (annotationName) {

                case "PathVariable":
                    result.add(
                            createParameter(
                                    parameter,
                                    annotation,
                                    ApiParameter.Location.PATH
                            )
                    );
                    break;

                case "RequestParam":
                    result.add(
                            createParameter(
                                    parameter,
                                    annotation,
                                    ApiParameter.Location.QUERY
                            )
                    );
                    break;

                case "RequestHeader":
                    result.add(
                            createParameter(
                                    parameter,
                                    annotation,
                                    ApiParameter.Location.HEADER
                            )
                    );
                    break;

                case "RequestBody":
                    result.add(
                            createParameter(
                                    parameter,
                                    annotation,
                                    ApiParameter.Location.BODY
                            )
                    );
                    break;

                default:
                    break;
            }
        }

        result.sort(
                Comparator
                        .comparingInt(
                                (ApiParameter parameter) ->
                                        parameter.getLocation().ordinal()
                        )
                        .thenComparing(
                                ApiParameter::getName
                        )
        );
        return result;
    }

    private ApiParameter createParameter(
            PsiParameter parameter,
            PsiAnnotation annotation,
            ApiParameter.Location location
    ) {

        String fallback =
                parameter.getName();

        if (fallback == null) {
            fallback = "parameter";
        }

        String name =
                extractAnnotationValue(
                        annotation,
                        fallback
                );

        String type =
                parameter.getType()
                        .getPresentableText();

        boolean required =
                extractRequired(
                        annotation,
                        true
                );

        return new ApiParameter(
                name,
                type,
                location,
                required
        );
    }

    private PsiAnnotation findRequestParameterAnnotation(
            PsiParameter parameter
    ) {

        for (PsiAnnotation annotation :
                parameter.getAnnotations()) {

            String name =
                    getSimpleAnnotationName(annotation);

            if (
                    "PathVariable".equals(name)
                            || "RequestParam".equals(name)
                            || "RequestHeader".equals(name)
                            || "RequestBody".equals(name)
            ) {
                return annotation;
            }
        }

        return null;
    }

    private String extractPath(
            PsiAnnotation annotation
    ) {

        String text =
                annotation.getText();

        Matcher named =
                NAMED_PATH_PATTERN.matcher(text);

        if (named.find()) {
            return named.group(1);
        }

        Matcher shorthand =
                SHORTHAND_PATH_PATTERN.matcher(text);

        if (shorthand.find()) {
            return shorthand.group(1);
        }

        return "";
    }

    private String extractHttpMethod(
            PsiAnnotation annotation
    ) {

        String text =
                annotation.getText();

        if (text.contains("RequestMethod.GET")) {
            return "GET";
        }

        if (text.contains("RequestMethod.POST")) {
            return "POST";
        }

        if (text.contains("RequestMethod.PUT")) {
            return "PUT";
        }

        if (text.contains("RequestMethod.DELETE")) {
            return "DELETE";
        }

        if (text.contains("RequestMethod.PATCH")) {
            return "PATCH";
        }

        return "ANY";
    }

    private String extractAnnotationValue(
            PsiAnnotation annotation,
            String fallback
    ) {

        Matcher matcher =
                ANNOTATION_NAME_PATTERN.matcher(
                        annotation.getText()
                );

        if (matcher.find()
                && !matcher.group(1).isBlank()) {

            return matcher.group(1);
        }

        return fallback;
    }

    private boolean extractRequired(
            PsiAnnotation annotation,
            boolean defaultValue
    ) {

        Matcher matcher =
                BOOLEAN_PATTERN.matcher(
                        annotation.getText()
                );

        if (matcher.find()) {
            return Boolean.parseBoolean(
                    matcher.group(1)
            );
        }

        return defaultValue;
    }

    public List<ApiEndpoint> scanPsiFile(
            PsiJavaFile psiFile
    ) {

        List<ApiEndpoint> result =
                new ArrayList<>();

        for (PsiClass psiClass :
                psiFile.getClasses()) {

            if (!isController(psiClass)) {
                continue;
            }

            String classPath =
                    getRequestMappingPath(psiClass);

            for (PsiMethod method :
                    psiClass.getMethods()) {

                List<Mapping> mappings =
                        getRequestMappings(method);

                for (Mapping mapping :
                        mappings) {

                    String fullPath =
                            combinePaths(
                                    classPath,
                                    mapping.path
                            );

                    String controller =
                            psiClass.getQualifiedName();

                    if (controller == null) {
                        controller =
                                psiClass.getName();
                    }

                    if (controller == null) {
                        controller =
                                "UnknownController";
                    }

                    result.add(
                            new ApiEndpoint(
                                    mapping.httpMethod,
                                    fullPath,
                                    controller,
                                    method.getName(),
                                    extractParameters(method),
                                    method.getReturnType() != null
                                            ? method.getReturnType()
                                              .getPresentableText()
                                            : "void"
                            )
                    );
                }
            }
        }

        return result;
    }

    public NavigationTarget findNavigationTarget(
            String controllerQualifiedName,
            String methodName
    ) {

        GlobalSearchScope scope =
                GlobalSearchScope.projectScope(project);

        Collection<VirtualFile> javaFiles =
                FileTypeIndex.getFiles(
                        JavaFileType.INSTANCE,
                        scope
                );

        PsiManager psiManager =
                PsiManager.getInstance(project);

        for (VirtualFile virtualFile : javaFiles) {

            PsiFileResult result =
                    findMethodInFile(
                            psiManager,
                            virtualFile,
                            controllerQualifiedName,
                            methodName
                    );

            if (result != null) {
                return new NavigationTarget(
                        result.file,
                        result.offset
                );
            }
        }

        return null;
    }

    private PsiFileResult findMethodInFile(
            PsiManager psiManager,
            VirtualFile virtualFile,
            String controllerQualifiedName,
            String methodName
    ) {

        PsiJavaFile psiFile =
                findJavaFile(
                        psiManager,
                        virtualFile
                );

        if (psiFile == null) {
            return null;
        }

        for (PsiClass psiClass :
                psiFile.getClasses()) {

            String qualifiedName =
                    psiClass.getQualifiedName();

            if (!controllerQualifiedName.equals(
                    qualifiedName
            )) {
                continue;
            }

            for (PsiMethod method :
                    psiClass.getMethods()) {

                if (methodName.equals(
                        method.getName()
                )) {

                    return new PsiFileResult(
                            virtualFile,
                            method.getTextOffset()
                    );
                }
            }
        }

        return null;
    }

    private static final class PsiFileResult {

        private final VirtualFile file;
        private final int offset;

        private PsiFileResult(
                VirtualFile file,
                int offset
        ) {
            this.file = file;
            this.offset = offset;
        }
    }

    public static final class NavigationTarget {

        private final VirtualFile file;
        private final int offset;

        public NavigationTarget(
                VirtualFile file,
                int offset
        ) {
            this.file = file;
            this.offset = offset;
        }

        public VirtualFile getFile() {
            return file;
        }

        public int getOffset() {
            return offset;
        }
    }

    private String getSimpleAnnotationName(
            PsiAnnotation annotation
    ) {

        String qualifiedName =
                annotation.getQualifiedName();

        if (qualifiedName == null) {
            return null;
        }

        return qualifiedName.substring(
                qualifiedName.lastIndexOf('.') + 1
        );
    }

    private boolean isAnnotation(
            PsiAnnotation annotation,
            String expected
    ) {

        return expected.equals(
                getSimpleAnnotationName(annotation)
        );
    }

    private String combinePaths(
            String classPath,
            String methodPath
    ) {

        String first =
                classPath.trim().replaceAll(
                        "^/+|/+$",
                        ""
                );

        String second =
                methodPath.trim().replaceAll(
                        "^/+|/+$",
                        ""
                );

        if (first.isEmpty()
                && second.isEmpty()) {
            return "/";
        }

        if (first.isEmpty()) {
            return "/" + second;
        }

        if (second.isEmpty()) {
            return "/" + first;
        }

        return "/" + first + "/" + second;
    }

    private static final class Mapping {

        private final String httpMethod;
        private final String path;

        private Mapping(
                String httpMethod,
                String path
        ) {
            this.httpMethod = httpMethod;
            this.path = path;
        }
    }
}