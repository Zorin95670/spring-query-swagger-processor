package io.github.zorin95670.processor.generator;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.github.zorin95670.processor.annotation.QueryFilter;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

import java.util.List;

import static com.squareup.javapoet.JavaFile.*;

/**
 * Concrete implementation of {@link QueryFilterGenerator} responsible for generating
 * query filter DTO classes.
 * <p>
 * This generator produces a DTO where:
 * </p>
 * <ul>
 *     <li>Each field annotated with {@link QueryFilterField} is mapped to a {@code List<String>}</li>
 *     <li>Each field is annotated with Swagger / OpenAPI {@code @Parameter}</li>
 *     <li>The class itself is annotated with {@code @Schema}</li>
 * </ul>
 *
 * <p>
 * Although generated fields are of type {@link String}, their filtering semantics
 * (operators, parsing, etc.) are determined by {@link QueryFilterField#type()}.
 * </p>
 *
 * <p>
 * Supported filtering operators include equality, comparison, range, pattern matching,
 * negation, and logical OR. These are documented through generated Swagger examples.
 * </p>
 *
 * <p>
 * If at least one field is of type {@code java.util.Date}, an additional {@code dateFormat}
 * parameter is generated to allow clients to specify the expected date format.
 * </p>
 */
public class QueryFilterDtoGenerator extends AbstractQueryFilterGenerator {

    /**
     * Creates a new DTO generator with the required processing utilities.
     *
     * @param filer utility for writing generated files
     * @param elementUtils utility for inspecting elements
     * @param processingEnv the annotation processing environment
     */
    public QueryFilterDtoGenerator(Filer filer, Elements elementUtils, ProcessingEnvironment processingEnv) {
        super(filer, elementUtils, processingEnv);
    }

    /**
     * Generates the default name of the DTO based on the annotated class.
     *
     * @param typeElement the annotated class
     * @return the generated DTO name (e.g., {@code MyEntityQueryFilterDto})
     */
    public static String generateName(TypeElement typeElement) {
        return typeElement.getSimpleName() + "QueryFilterDto";
    }

    /**
     * Generates the query filter DTO class.
     * <p>
     * The generated class includes:
     * </p>
     * <ul>
     *     <li>A {@code @Schema} annotation with a description derived from {@link QueryFilter#description()}</li>
     *     <li>One field per {@link QueryFilterField}, each as {@code List<String>}</li>
     *     <li>Swagger {@code @Parameter} annotations with examples for each field</li>
     *     <li>Standard getter and setter methods</li>
     *     <li>An optional {@code dateFormat} field if date filtering is detected</li>
     * </ul>
     *
     * @param clazz the annotated class
     * @param annotation the {@link QueryFilter} annotation instance
     * @param name the name of the generated DTO class
     */
    @Override
    public void generate(TypeElement clazz, QueryFilter annotation, String name) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "FilterProcessor triggered DTO");
        String packageName = elementUtils.getPackageOf(clazz).getQualifiedName().toString();

        ClassName schemaAnnotation = ClassName.get("io.swagger.v3.oas.annotations.media", "Schema");

        TypeSpec.Builder builder = TypeSpec.classBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(AnnotationSpec.builder(schemaAnnotation)
                .addMember("description", "$S", String.format(annotation.description(), clazz.getSimpleName()))
                .build());

        boolean hasDateField = false;

        for (Element field : getFilterFields(clazz)) {
            String fieldName = field.getSimpleName().toString();
            TypeName fieldType = ParameterizedTypeName.get(
                ClassName.get(List.class),
                ClassName.get(String.class)
            );

            builder.addField(
                FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE)
                    .addAnnotation(buildSchemaAnnotation(field))
                    .build()
            );

            if ("java.util.Date".equals(resolveTypeName(field))) {
                hasDateField = true;
            }

            builder.addMethod(MethodSpec.methodBuilder("get" + capitalize(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .returns(fieldType)
                .addStatement("return this.$N", fieldName)
                .build());

            builder.addMethod(MethodSpec.methodBuilder("set" + capitalize(fieldName))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(fieldType, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build());
        }

        if (hasDateField) {
            ClassName parameterAnnotation = ClassName.get("io.swagger.v3.oas.annotations", "Parameter");
            ClassName exampleObject = ClassName.get("io.swagger.v3.oas.annotations.media", "ExampleObject");

            AnnotationSpec dateFormatSchema = AnnotationSpec.builder(parameterAnnotation)
                .addMember("description", "$S",
                    "By default, filtering dates will use a timestamp. " +
                        "You can specify a custom date format by including this parameter in your request. " +
                        "For valid date format patterns, refer to the Java DateFormat documentation.")
                .addMember("examples", "$L",
                    buildExampleObject(exampleObject, "Year/Month/Day", "yyyyMMdd"))
                .addMember("examples", "$L",
                    buildExampleObject(exampleObject, "Month/Day/Year with slashes", "MM/dd/yyyy"))
                .addMember("examples", "$L",
                    buildExampleObject(exampleObject, "Day/Month/Year with dashes", "dd-MM-yyyy"))
                .build();

            builder.addField(
                FieldSpec.builder(ClassName.get(String.class), "dateFormat", Modifier.PRIVATE)
                    .addAnnotation(dateFormatSchema)
                    .build()
            );

            builder.addMethod(MethodSpec.methodBuilder("getDateFormat")
                .addModifiers(Modifier.PUBLIC)
                .returns(ClassName.get(String.class))
                .addStatement("return this.dateFormat")
                .build());

            builder.addMethod(MethodSpec.methodBuilder("setDateFormat")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(ClassName.get(String.class), "dateFormat")
                .addStatement("this.dateFormat = dateFormat")
                .build());
        }

        writeFile(builder(packageName, builder.build()).build());
    }
}
