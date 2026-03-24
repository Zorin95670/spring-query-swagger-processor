package io.github.zorin95670.processor.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a class as a source for generating query filter artifacts.
 * <p>
 * When applied to a type, this annotation triggers the annotation processor
 * to generate a corresponding DTO used to handle HTTP query parameters
 * for filtering resources.
 * </p>
 *
 * <p>
 * The generated DTO is enriched with Swagger / OpenAPI annotations in order to
 * automatically document the available query parameters in API specifications.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface QueryFilter {
    /**
     * Defines the name of the generated artifact.
     * <p>
     * If left empty, a default name will be derived from the annotated class.
     * </p>
     *
     * @return the explicit name of the generated class, or empty for default resolution
     */
    String name() default "";

    /**
     * Specifies the type of artifact to generate.
     * <p>
     * This allows selecting different generation strategies.
     * Currently, only {@link Type#DTO} is supported.
     * </p>
     *
     * @return the generation type
     */
    Type type() default Type.DTO;

    /**
     * Defines the Swagger / OpenAPI description of the generated DTO class.
     * <p>
     * This value is injected into the generated class-level documentation
     * (e.g., via {@code @Schema(description = ...)}).
     * </p>
     *
     * <p>
     * The placeholder {@code %s} can be used to dynamically insert the
     * resource name during generation.
     * </p>
     *
     * @return the Swagger/OpenAPI description for the generated DTO
     */
    String description() default "HTTP query parameters for filtering %s resources";

    /**
     * Enumeration of supported generation types.
     */
    enum Type {

        /**
         * Indicates that a Data Transfer Object (DTO) should be generated.
         * <p>
         * The generated DTO includes Swagger / OpenAPI annotations to describe
         * query parameters for API documentation.
         * </p>
         */
        DTO
    }
}
