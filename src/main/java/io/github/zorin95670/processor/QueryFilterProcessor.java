package io.github.zorin95670.processor;

import com.google.auto.service.AutoService;
import io.github.zorin95670.processor.annotation.QueryFilter;
import io.github.zorin95670.processor.generator.QueryFilterDtoGenerator;
import io.github.zorin95670.processor.generator.QueryFilterGenerator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.util.Set;

/**
 * Annotation processor responsible for handling QueryFilter annotations.
 * <p>
 * This processor scans all classes annotated with QueryFilter during
 * compilation and delegates the generation of corresponding DTO classes
 * to a QueryFilterGenerator implementation.
 * </p>
 *
 * <p>
 * It uses the Java Annotation Processing API to inspect annotated elements
 * and generate additional source files via the Filer.
 * </p>
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("io.github.zorin95670.processor.annotation.QueryFilter")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
public class QueryFilterProcessor extends AbstractProcessor {

    /**
     * Utility used to create new source, class, or resource files.
     */
    private Filer filer;

    /**
     * Utility providing access to program elements such as packages,
     * types, and their members.
     */
    private Elements elementUtils;

    /**
     * Default constructor.
     */
    public QueryFilterProcessor() {
        super();
    }
    /**
     * Initializes the processor with the given processing environment.
     *
     * @param processingEnv the environment provided by the annotation processing tool
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        filer = processingEnv.getFiler();
        elementUtils = processingEnv.getElementUtils();
    }

    /**
     * Processes all elements annotated with QueryFilter.
     * <p>
     * For each annotated class, this method:
     * <ul>
     *     <li>Validates that the element is a class</li>
     *     <li>Retrieves the QueryFilter annotation</li>
     *     <li>Resolves the target DTO name</li>
     *     <li>Delegates code generation to a QueryFilterGenerator</li>
     * </ul>
     *
     * @param annotations the set of annotations requested to be processed
     * @param roundEnv the environment for information about the current and prior round
     * @return {@code true} to indicate that the annotations have been processed
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "FilterProcessor triggered");

        for (Element element : roundEnv.getElementsAnnotatedWith(QueryFilter.class)) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }

            TypeElement typeElement = (TypeElement) element;
            QueryFilter annotation = typeElement.getAnnotation(QueryFilter.class);

            if (annotation == null) {
                continue;
            }

            String dtoName = resolveName(annotation, typeElement);

            QueryFilterGenerator generator = resolveGenerator(annotation);
            generator.generate(typeElement, annotation, dtoName);
        }
        return true;
    }

    /**
     * Resolves the generator implementation to use for the given annotation.
     * <p>
     * Currently returns a QueryFilterDtoGenerator, but can be extended
     * to support multiple generator strategies.
     *
     * @param annotation the QueryFilter annotation instance
     * @return a configured QueryFilterGenerator
     */
    private QueryFilterGenerator resolveGenerator(QueryFilter annotation) {
        return new QueryFilterDtoGenerator(filer, elementUtils, processingEnv);
    }

    /**
     * Resolves the name of the generated DTO.
     * <p>
     * If a custom name is provided in the annotation, it is used.
     * Otherwise, a default name is generated based on the annotated type.
     *
     * @param annotation the QueryFilter annotation
     * @param typeElement the annotated class element
     * @return the resolved DTO class name
     */
    private String resolveName(QueryFilter annotation, TypeElement typeElement) {
        if (!annotation.name().isEmpty()) {
            return annotation.name();
        }

        return QueryFilterDtoGenerator.generateName(typeElement);
    }
}
