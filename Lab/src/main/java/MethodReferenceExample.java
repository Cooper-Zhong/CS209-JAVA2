import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

interface A {
    int aMethod(String s);
}

public class MethodReferenceExample {
    public static void main(String[] args) {

        Stream<String> stream = Stream.of("1a", "1bb", "1c", "2a", "2a", "2bb");

        Map<Character, Set<String>> group =

                stream.collect(Collectors.groupingBy(s->s.charAt(0), Collectors.mapping(s->s.substring(1), Collectors.toSet())));
        System.out.println(group);
    }
}
