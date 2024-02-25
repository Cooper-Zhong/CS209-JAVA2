import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class main {
    public static void main(String[] args) {
//        Student student = new Student();
//        int[] scores = student.getScores();
//        System.out.println(Arrays.toString(scores));
//        scores[0] = 99;
//        System.out.println(Arrays.toString(student.getScores()));

//        Integer[] li = {1, 2, 3, 4, 5};
//        Pair<Integer> pi = Pair.minmax(li);
//        Pair<? extends Number> pBoundWild = pi;
//        Pair<? extends String> pBoundWild2;
//        Double[] ld = {1.0, 2.0, 3.0};
//        Pair<Double> pd = Pair.minmax(ld);
//        pBoundWild = pd;
//        String[] words = {"This", "is", "CS209a"};
//        Pair<String> ps = Pair.minmax(words);
//        pBoundWild = ps;

//        List<Integer> li = new ArrayList<>();
//        li.add(1);
//        li.add(null);
//        li.add(null);
//        li.add(2);
//        for (int i = 0; i<li.size(); i++) {
//            System.out.println("old: ");
//            System.out.println(li.get(i));
//            if (li.get(i) == null) {
//                li.remove(i);
//            }
//            System.out.println("new: ");
//            System.out.print(i);
//            System.out.println(li.get(i));
//        }
//        System.out.println(li);
//        Stream<Integer> evenNumber = Stream.iterate(2, n -> n + 2);
//        evenNumber.limit(10).forEach(System.out::println);
//
//        Integer[] array = new Integer[]{1,2,3};
//
//        Stream<Integer> istream = Stream.of(array);
//
//        Stream<String> sentence = Stream.of("This","is","Java","2");
//
//        istream.forEach(System.out::println);
//
//        sentence.forEach(System.out::println);
//        Stream<String> stream = Stream.of("a", "bb", "cc", "ddd", "a", "bb");
//
//        Map<String, Long> group =
//
//                stream.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
//        System.out.println(group);
//        Stream<String> stream = Stream.of("1a", "1bb", "1c", "2a", "2a", "2bb");
//
//        Map<Character, Set<String>> group =
//
//                stream.collect(Collectors.groupingBy(s->s.charAt(0), Collectors.mapping(s->s.substring(1), Collectors.toSet())));
//        System.out.println(group);

        Class cls1 = String.class;
//        Class cls2 = Class.forName("java.lang.String");
        Class cls3 = "hello".getClass();

//        System.out.println(cls1 == cls2);
        System.out.println(cls1 == cls3);

        String s = "Hello Java";
        try {
            Method m = String.class.getMethod("substring", int.class);
            System.out.println(m.invoke(s, 6));
            Method m2 = Integer.class.getMethod("parseInt", String.class);
            System.out.println(m2.invoke(null, "12345").getClass().getName());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}


class Pair<T> {

    private T first;

    private T second;

    public Pair() {
        first = null;
        second = null;
    }

    public Pair(T first, T second) {
        this.first = first;
        this.second = second;
    }

    public static <T extends Comparable> Pair<T> minmax(T[] a) {
        if (a == null || a.length == 0) return null;

        T min = a[0];

        T max = a[0];

        for (int i = 1; i<a.length; i++) {

            if (min.compareTo(a[i])>0) min = a[i];

            if (max.compareTo(a[i])<0) max = a[i];

        }
        return new Pair<>(min, max);
    }


    public T getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }

    public void setFirst(T newValue) {
        first = newValue;
    }

    public void setSecond(T newValue) {
        second = newValue;
    }

}

class Student {
    private int[] scores = new int[]{100, 90, 95};

    public int[] getScores() {
        return scores;

    }
}