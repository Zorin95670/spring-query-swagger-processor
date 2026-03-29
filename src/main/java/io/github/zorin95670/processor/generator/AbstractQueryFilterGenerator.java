package io.github.zorin95670.processor.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import io.github.zorin95670.processor.annotation.QueryFilterField;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base implementation of {@link QueryFilterGenerator} providing common utilities
 * for generating query filter DTOs.
 * <p>
 * This class centralizes reusable logic for:
 * </p>
 * <ul>
 *     <li>Resolving fields annotated with {@link QueryFilterField}, including inherited ones</li>
 *     <li>Writing generated source files באמצעות {@link Filer}</li>
 *     <li>Building Swagger / OpenAPI annotations for generated DTO fields</li>
 *     <li>Providing example values based on the semantic filter type</li>
 * </ul>
 *
 * <p>
 * Generated DTO fields are always of type {@link String}, but their behavior
 * (supported operators, parsing, etc.) is driven by the {@link QueryFilterField#type()}.
 * </p>
 */
public abstract class AbstractQueryFilterGenerator implements QueryFilterGenerator {

    /**
     * Used to create new source files during annotation processing.
     */
    protected final Filer filer;

    /**
     * Provides utilities for working with program elements.
     */
    protected final Elements elementUtils;

    /**
     * The current annotation processing environment.
     */
    protected final ProcessingEnvironment processingEnv;

    /**
     * Constructs a new generator with required processing utilities.
     *
     * @param filer utility for file creation
     * @param elementUtils utility for element inspection
     * @param processingEnv the processing environment
     */
    protected AbstractQueryFilterGenerator(Filer filer, Elements elementUtils, ProcessingEnvironment processingEnv) {
        this.filer = filer;
        this.elementUtils = elementUtils;
        this.processingEnv = processingEnv;
    }

    /**
     * Retrieves all fields annotated with {@link QueryFilterField} from the given class,
     * including fields declared in its superclass hierarchy.
     *
     * @param clazz the root class to inspect
     * @return a list of annotated field elements
     */
    protected List<? extends Element> getFilterFields(TypeElement clazz) {
        List<Element> fields = new ArrayList<>();

        TypeElement current = clazz;
        while (current != null) {
            current.getEnclosedElements().stream()
                .filter(e -> e.getKind() == ElementKind.FIELD)
                .filter(e -> e.getAnnotation(QueryFilterField.class) != null)
                .forEach(fields::add);

            TypeMirror superclass = current.getSuperclass();
            if (superclass.getKind() == TypeKind.NONE || superclass.getKind() == TypeKind.ERROR) break;

            current = (TypeElement) ((DeclaredType) superclass).asElement();
        }

        return fields;
    }

    /**
     * Writes the generated {@link JavaFile} to the filer.
     *
     * @param javaFile the file to write
     */
    protected void writeFile(JavaFile javaFile) {
        try {
            javaFile.writeTo(filer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Capitalizes the first character of the given string.
     *
     * @param str the input string
     * @return the capitalized string, or the original value if null/empty
     */
    protected String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Builds a Swagger / OpenAPI {@code @Parameter} annotation for a generated DTO field.
     * <p>
     * The annotation includes:
     * </p>
     * <ul>
     *     <li>Description and required flag from {@link QueryFilterField}</li>
     *     <li>Example values illustrating supported filtering operators</li>
     * </ul>
     *
     * @param field the annotated field
     * @return the constructed {@link AnnotationSpec}
     */
    protected AnnotationSpec buildSchemaAnnotation(Element field) {
        QueryFilterField annotation = field.getAnnotation(QueryFilterField.class);
        ClassName parameterAnnotation = ClassName.get("io.swagger.v3.oas.annotations", "Parameter");
        ClassName exampleObject = ClassName.get("io.swagger.v3.oas.annotations.media", "ExampleObject");

        AnnotationSpec.Builder builder = AnnotationSpec.builder(parameterAnnotation)
            .addMember("description", "$S", annotation.description())
            .addMember("required", "$L", annotation.required());

        List<AnnotationSpec> examples = buildExamples(field, exampleObject);
        examples.forEach(e -> builder.addMember("examples", "$L", e));

        return builder.build();
    }

    /**
     * Builds a list of Swagger {@code @ExampleObject} annotations based on the
     * semantic type of the field.
     * <p>
     * Examples demonstrate supported filtering operators such as equality,
     * comparison, range, pattern matching, negation, and logical OR.
     * </p>
     *
     * @param field the annotated field
     * @param exampleObject the Swagger ExampleObject class reference
     * @return a list of example annotations
     */
    private List<AnnotationSpec> buildExamples(Element field, ClassName exampleObject) {
        return switch (resolveTypeName(field)) {
            case "java.lang.String" -> List.of(
                buildExampleObject(exampleObject, "Filter by exact value",     "john"),
                buildExampleObject(exampleObject, "Exclude exact value", "not_john"),
                buildExampleObject(exampleObject, "Filter by partial match",       "lk_*john*"),
                buildExampleObject(exampleObject, "Exclude partial match",   "not_lk_*john*"),
                buildExampleObject(exampleObject, "Combine multiple filters",         "lk_*john*|john|not_jacque|not_lk_*jac*")
            );
            case "java.lang.Integer", "java.lang.Long",
                 "java.lang.Float",   "java.lang.Double" -> List.of(
                buildExampleObject(exampleObject, "Filter by exact value",       "42"),
                buildExampleObject(exampleObject, "Exclude exact value",   "not_42"),
                buildExampleObject(exampleObject, "Filter values greater than", "gt_42"),
                buildExampleObject(exampleObject, "Exclude values greater than", "not_gt_42"),
                buildExampleObject(exampleObject, "Filter values lesser than",  "lt_42"),
                buildExampleObject(exampleObject, "Exclude values lesser than",  "not_lt_42"),
                buildExampleObject(exampleObject, "Filter values between two numbers",      "21_bt_42"),
                buildExampleObject(exampleObject, "Exclude values between two numbers",      "not_21_bt_42"),
                buildExampleObject(exampleObject, "Combine multiple filters",           "gt_10|lt_5")
            );
            case "java.util.Date" -> List.of(
                buildExampleObject(exampleObject, "Filter by exact date",       "2024-01-01"),
                buildExampleObject(exampleObject, "Exclude exact date",   "not_2024-01-01"),
                buildExampleObject(exampleObject, "Filter dates after", "gt_2024-01-01"),
                buildExampleObject(exampleObject, "Exclude dates after", "not_gt_2024-01-01"),
                buildExampleObject(exampleObject, "Filter dates before",  "lt_2024-01-01"),
                buildExampleObject(exampleObject, "Exclude dates before",  "not_lt_2024-01-01"),
                buildExampleObject(exampleObject, "Filter dates between two dates",      "2024-01-01_bt_2024-12-31"),
                buildExampleObject(exampleObject, "Exclude dates between two dates",      "not_2024-01-01_bt_2024-12-31"),
                buildExampleObject(exampleObject, "Combine multiple filters",           "gt_2024-01-01|lt_2023-01-01")
            );
            default -> List.of(
                buildExampleObject(exampleObject, "Filter by exact value",     "value"),
                buildExampleObject(exampleObject, "Exclude exact value", "not_value"),
                buildExampleObject(exampleObject, "Combine multiple filters",           "value1|value2")
            );
        };
    }

    /**
     * Resolves the fully qualified name of the type declared in
     * {@link QueryFilterField#type()} without triggering class loading.
     *
     * @param field the annotated field
     * @return the fully qualified type name
     */
    protected String resolveTypeName(Element field) {
        try {
            field.getAnnotation(QueryFilterField.class).type();
            throw new IllegalStateException("unreachable");
        } catch (MirroredTypeException e) {
            return e.getTypeMirror().toString();
        }
    }

    /**
     * Builds a Swagger {@code @ExampleObject}.
     *
     * @param exampleObject the Swagger ExampleObject class reference
     * @param name the example name
     * @param value the example value
     * @return the constructed annotation
     */
    protected AnnotationSpec buildExampleObject(ClassName exampleObject, String name, String value) {
        return AnnotationSpec.builder(exampleObject)
            .addMember("name", "$S", name)
            .addMember("value", "$S", value)
            .build();
    }
}
