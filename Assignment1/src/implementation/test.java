package implementation;

import declaration.stock.Field;

import java.nio.charset.StandardCharsets;
import java.security.Principal;

public class test {
    public static void main(String[] args) {
        byte[] stk = "Stkcd".getBytes(StandardCharsets.UTF_8);
        for (byte b : stk) {
            System.out.println(b);
        }
        for (int i = 0; i<Field.values().length; i++) {
            System.out.println(Field.values()[i]);
        }
    }
}
