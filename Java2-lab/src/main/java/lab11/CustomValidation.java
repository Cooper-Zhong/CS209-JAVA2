package lab11;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(CustomValidations.class)
@interface CustomValidation {
    Rule rule();
}