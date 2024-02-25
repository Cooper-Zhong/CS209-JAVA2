package lab11;

import java.lang.reflect.Field;
import java.util.Scanner;

public class DataValidator {
    public static boolean validate(Object obj) {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();
        boolean isValid = true;

        for (Field field : fields) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(MinLength.class)) {
                MinLength minLengthAnnotation = field.getAnnotation(MinLength.class);
                int minLength = minLengthAnnotation.min();
                try {
                    String value = (String) field.get(obj);
                    if (value.length()<minLength) {
                        System.out.println("Validation failed for field *" + field.getName() + "*: should have a minimum length of " + minLength);
                        isValid = false;
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

            if (field.isAnnotationPresent(CustomValidations.class)) {
                CustomValidation[] validations = field.getAnnotationsByType(CustomValidation.class);
                try {
                    String value = (String) field.get(obj);

                    for (CustomValidation validation : validations) {
                        switch (validation.rule()) {
                            case ALL_LOWERCASE:
                                if (!value.equals(value.toLowerCase())) {
                                    System.out.println("Validation failed for field *" + field.getName() + "*: should be all lowercase");
                                    isValid = false;
                                }
                                break;
                            case NO_USERNAME:
                                // Check if the password contains the username
                                Field usernameField = clazz.getDeclaredField("username");
                                usernameField.setAccessible(true);
                                if (value.contains((String) usernameField.get(obj))) {
                                    System.out.println("Validation failed for field *" + field.getName() + "*: should not contain username");
                                    isValid = false;
                                }
                                break;
                            case HAS_BOTH_DIGITS_AND_LETTERS:
                                // Check if the password contains both letters and digits
                                if (!value.matches("^(?=.*[a-zA-Z])(?=.*[0-9]).*$")) {
                                    System.out.println("Validation failed for field *" + field.getName() + "*: should have both letters and digits");
                                    isValid = false;
                                }
                                break;
                            default:
                                break;
                        }
                    }
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    e.printStackTrace();
                }
            }
        }

        return isValid;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print("Username: ");
            String username = sc.next();
            System.out.print("Password: ");
            String pwd = sc.next();
            User user = new User(username, pwd);
            if (validate(user)) {
                System.out.println("Success!");
                break;
            }
        }
    }
}
