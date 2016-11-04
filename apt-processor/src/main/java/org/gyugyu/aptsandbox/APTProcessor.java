package org.gyugyu.aptsandbox;

import android.content.Context;
import android.content.Intent;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;

@AutoService(Processor.class)
public class APTProcessor extends AbstractProcessor {
    private Filer filer;

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
        final String className = String.format("%sIntentBuilder", element.getSimpleName());
        TypeSpec.Builder builder = TypeSpec.classBuilder(className)
                .addSuperinterface(TestInterface.class)
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
        builder.addField(Intent.class, "intent", Modifier.PRIVATE);

        MethodSpec.Builder constructor = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Context.class, "context")
                .addStatement("intent = new Intent(context, $T.class)", TypeName.get(element.asType()));
        builder.addMethod(constructor.build());

        MethodSpec.Builder fooMethod = MethodSpec.methodBuilder("foo")
                .addModifiers(Modifier.PUBLIC)
                .returns(TestInterface.class)
                .addParameter(String.class, "bar")
                .addStatement("intent.putExtra($S, $N)", "bar", "bar")
                .addStatement("return this");
        builder.addMethod(fooMethod.build());

        return builder.build();
    }

    private String getPackageName(Element e) {
        while (!(e instanceof PackageElement)) {
            e = e.getEnclosingElement();
        }
        return ((PackageElement)e).getQualifiedName().toString();
    }
}
