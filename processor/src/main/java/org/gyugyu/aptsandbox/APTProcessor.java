package org.gyugyu.aptsandbox;

import android.content.Context;
import android.content.Intent;

import com.google.auto.service.AutoService;
import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import javax.lang.model.util.Types;

import static com.google.auto.common.AnnotationMirrors.getAnnotationValue;
import static com.google.auto.common.MoreElements.getAnnotationMirror;

@AutoService(Processor.class)
public class APTProcessor extends AbstractProcessor {
    private Filer filer;
    private Types typeUtil;


    private static final AnnotationValueVisitor<ImmutableList<TypeMirror>, Void> TO_LIST_OF_TYPE = new SimpleAnnotationValueVisitor6<ImmutableList<TypeMirror>, Void>() {
        @Override
        public ImmutableList<TypeMirror> visitArray(List<? extends AnnotationValue> vals, Void aVoid) {
            return FluentIterable.from(vals).transform(new Function<AnnotationValue, TypeMirror>() {
                @Override
                public TypeMirror apply(AnnotationValue input) {
                    return TO_TYPE.visit(input);
                }
            }).toList();
        }
    };

    private static final AnnotationValueVisitor<TypeMirror, Void> TO_TYPE = new SimpleAnnotationValueVisitor6<TypeMirror, Void>() {
        @Override
        public TypeMirror visitType(TypeMirror t, Void aVoid) {
            return t;
        }
    };

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return new HashSet<>(Collections.singletonList(
                TestAnnotation.class.getCanonicalName()
        ));
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_7;
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        super.init(env);
        filer = env.getFiler();
        typeUtil = env.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(TestAnnotation.class)) {
            TypeSpec typeSpec = buildTypeSpec(element);
            JavaFile javaFile = JavaFile.builder(getPackageName(element), typeSpec).build();
            try {
                javaFile.writeTo(filer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private TypeSpec buildTypeSpec(Element element) {
        AnnotationMirror annotationMirror = getAnnotationMirror(element, TestAnnotation.class).get();
        AnnotationValue annotationValue = getAnnotationValue(annotationMirror, "value");
        TypeMirror bean = (TypeMirror) annotationValue.getValue();
        TypeName beanName = TypeName.get(bean);

        final String className = String.format("%sIntentBuilder", element.getSimpleName());
        TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                .addSuperinterface(TestInterface.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addField(Intent.class, "intent", Modifier.PRIVATE)
                .addField(beanName, "bean", Modifier.PRIVATE);


        List<ExecutableElement> constructors = new ArrayList<>();
        List<ExecutableElement> getters = new ArrayList<>();
        for (Element e : ((TypeElement) typeUtil.asElement(bean)).getEnclosedElements()) {
            if (e instanceof ExecutableElement) {
                if (e.getKind() == ElementKind.CONSTRUCTOR) {
                    constructors.add((ExecutableElement) e);
                }
                if (e.getSimpleName().toString().indexOf("get") == 0) {
                    getters.add((ExecutableElement) e);
                }
            }
        }

        for (ExecutableElement executable : constructors) {
            MethodSpec.Builder c = MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Context.class, "context");

            StringBuilder statement = new StringBuilder("bean = new $T(");
            List<? extends VariableElement> parameters = executable.getParameters();
            int len = parameters.size();
            for (int i = 0; i < len; i++) {
                VariableElement parameter = parameters.get(i);
                c.addParameter(TypeName.get(parameter.asType()), parameter.getSimpleName().toString());
                statement.append(parameter.getSimpleName().toString());

                if (i + 1 != len) {
                    statement.append(", ");
                }
            }
            statement.append(")");

            c.addStatement(statement.toString(), beanName)
                .addStatement("intent = new Intent(context, $T.class)", TypeName.get(element.asType()));
            builder.addMethod(c.build());
        }

        MethodSpec.Builder fooMethod = MethodSpec.methodBuilder("foo")
                .addModifiers(Modifier.PUBLIC)
                .returns(TestInterface.class)
                .addParameter(String.class, "bar")
                .addStatement("intent.putExtra($S, $N)", "bar", "bar")
                .addStatement("return this");
        builder.addMethod(fooMethod.build());

        MethodSpec.Builder buildMethod = MethodSpec.methodBuilder("build")
                .addModifiers(Modifier.PUBLIC)
                .returns(Intent.class);

        for (ExecutableElement getter : getters) {
            Extra extra = getter.getAnnotation(Extra.class);
            Name name = getter.getSimpleName();
            buildMethod.beginControlFlow("if (bean.$N() != null)", name)
                    .addStatement("intent.putExtra($S, bean.$N())", extra.value(), getter.getSimpleName())
                    .endControlFlow();
        }

        buildMethod.addStatement("return intent");
        builder.addMethod(buildMethod.build());

        return builder.build();
    }

    private String getPackageName(Element e) {
        while (!(e instanceof PackageElement)) {
            e = e.getEnclosingElement();
        }
        return ((PackageElement) e).getQualifiedName().toString();
    }
}
