package org.example;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!");
        List<String> list = new ArrayList<>();
        list.add("a");
        list.add("b");

        System.out.println(list);
        add(list);
        System.out.println(list);
    }

    public static void add(List<String> list) {
        list.add("d");
    }
}