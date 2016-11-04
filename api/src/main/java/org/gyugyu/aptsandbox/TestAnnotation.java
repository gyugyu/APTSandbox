package org.gyugyu.aptsandbox;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
public @interface TestAnnotation {
    Class value() default Null.class;

    static final class Null {}
}
