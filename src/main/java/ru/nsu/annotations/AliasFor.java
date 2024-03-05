package ru.nsu.annotations;


import java.lang.annotation.*;

/**
 * You can use this annotation on your own annotations if you want to make them compatible with MagicInjector.
 * This annotation can be used to create aliases for {@link Autowired}, {@link PostConstruct}, {@link PreDestroy}.
 * <p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface AliasFor {
    Class<? extends Annotation> value();
}
