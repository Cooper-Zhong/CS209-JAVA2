package lab4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Practice4 {
    public static class City {
        private String name;
        private String state;
        private int population;

        public int getPopulation() {
            return population;
        }


        public City(String name, String state, int population) {
            this.name = name;
            this.state = state;
            this.population = population;
        }

        @Override
        public String toString() {
            return "City{" +
                    "name='" + name + '\'' +
                    ", state='" + state + '\'' +
                    ", population=" + population +
                    '}';
        }

        public String getName() {
            return name;
        }

        public String getState() {
            return state;
        }


    }

    public static Stream<City> readCities(String filename) throws IOException {
        return Files.lines(Paths.get(filename))
                .map(l -> l.split(", "))
                .map(a -> new City(a[0], a[1], Integer.parseInt(a[2])));
    }

    public static void main(String[] args) throws IOException {

        Stream<City> cities = readCities("/Users/cooperz/SUSTech/2023_Fall/CS209_Java2/CS209-JAVA2-repo/Java2-lab/src/main/java/lab4/cities.txt");

        // Q1: count how many cities there are for each state
        // TODO: Map<String, Long> cityCountPerState = ...
        Map<String, Long> cityCountPerState = cities.collect(
                Collectors.groupingBy(City::getState, Collectors.counting()));
        System.out.println(cityCountPerState);
        System.out.println("----------------");


        cities = readCities("/Users/cooperz/SUSTech/2023_Fall/CS209_Java2/CS209-JAVA2-repo/Java2-lab/src/main/java/lab4/cities.txt");
        // Q2: count the total population for each state
        // TODO: Map<String, Integer> statePopulation = ...
        Map<String, Integer> statePopulation = cities.collect(
                Collectors.groupingBy(City::getState, Collectors.summingInt(City::getPopulation)));
        System.out.println(statePopulation);
        System.out.println("----------------");


        cities = readCities("/Users/cooperz/SUSTech/2023_Fall/CS209_Java2/CS209-JAVA2-repo/Java2-lab/src/main/java/lab4/cities.txt");
        // Q3: for each state, get the city with the longest name
        // TODO: Map<String, String> longestCityNameByState = ...
        Map<String, String> longestCityNameByState = cities.collect(
                Collectors.groupingBy(City::getState, Collectors.collectingAndThen(
                        Collectors.maxBy((a, b) -> a.getName().length() - b.getName().length()), city -> city.get().getName())));
        System.out.println(longestCityNameByState);
        System.out.println("----------------");


        cities = readCities("/Users/cooperz/SUSTech/2023_Fall/CS209_Java2/CS209-JAVA2-repo/Java2-lab/src/main/java/lab4/cities.txt");
        // Q4: for each state, get the set of cities with >500,000 population
        // TODO: Map<String, Set<City>> largeCitiesByState = ...
        Map<String, Set<City>> largeCitiesByState = cities.collect(
                Collectors.groupingBy(City::getState, Collectors.filtering(
                        city -> city.getPopulation()>500000, Collectors.toSet())));

//        System.out.println(largeCitiesByState);
        for (Map.Entry<String, Set<City>> entry : largeCitiesByState.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }
    }
}
