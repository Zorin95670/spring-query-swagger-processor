package io.github.zorin95670.processor.generator;

import io.github.zorin95670.processor.annotation.QueryFilter;

import javax.lang.model.element.TypeElement;

/**
 * Functional contract for generating query filter artifacts from annotated classes.
 * <p>
 * Implementations of this interface are responsible for producing source code
 * (typically DTOs) based on a class annotated with QueryFilter.
 * </p>
 *
 * <p>
 * The generation process typically involves:
 * </p>
 * <ul>
 *     <li>Inspecting the annotated class and its QueryFilterField fields</li>
 *     <li>Deriving metadata from annotations</li>
 *     <li>Generating a corresponding representation (e.g., a DTO)</li>
 *     <li>Enriching the generated code with Swagger / OpenAPI annotations</li>
 * </ul>
 *
 * <p>
 * This interface is marked as a FunctionalInterface, allowing implementations
 * to be provided as lambda expressions if desired.
 * </p>
 */
@FunctionalInterface
public interface QueryFilterGenerator {
    /**
     * Generates a query filter artifact for the given annotated class.
     *
     * @param clazz the class annotated with QueryFilter
     * @param annotation the QueryFilter annotation instance
     * @param name the name of the generated artifact
     */
    void generate(TypeElement clazz, QueryFilter annotation, String name);
}
