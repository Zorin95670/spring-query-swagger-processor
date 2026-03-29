package io.github.zorin95670.processor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to describe a field as part of a generated query filter DTO.
 * <p>
 * Each annotated field contributes to the generation of a query parameter
 * represented as a {@link String} in the generated DTO. The actual filtering
 * behavior is driven by the declared {@link #type()}, which defines the
 * allowed operators and how the value is interpreted when building the query.
 * </p>
 *
 * <p>
 * The generated field is enriched with Swagger / OpenAPI annotations to
 * document available query parameters and their expected format.
 * </p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface QueryFilterField {
    /**
     * Defines the semantic type of the field being filtered in the data source.
     * <p>
     * This type does <b>not</b> impact the generated DTO field type (which is always
     * {@link String}), but determines:
     * </p>
     * <ul>
     *     <li>The set of supported filtering operators</li>
     *     <li>How the value is parsed and interpreted</li>
     *     <li>The resulting query construction logic</li>
     * </ul>
     *
     * @return the underlying data type used for filtering
     */
    Class<?> type();

    /**
     * Defines the Swagger / OpenAPI description of the query parameter.
     * <p>
     * This value is injected into the generated field-level documentation
     * (e.g., via {@code @Schema(description = ...)}).
     * </p>
     *
     * @return the description of the query parameter
     */
    String description() default "";

    /**
     * Indicates whether the query parameter is required.
     * <p>
     * This affects the generated OpenAPI specification and may also be used
     * for validation purposes.
     * </p>
     *
     * @return {@code true} if the parameter is mandatory, {@code false} otherwise
     */
    boolean required() default false;
}
