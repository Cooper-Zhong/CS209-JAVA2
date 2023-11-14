package lab6;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;


public class prac6 {
    public static void main(String[] args) throws IOException {
        String srczipPath = "/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/src.zip"; // Update with the actual path to src.zip
        String rtjarPath = "/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/jre/lib/rt.jar";

        Map<String, Integer> javaCounts = new HashMap<>();
        Map<String, Integer> classCounts = new HashMap<>();
        Set<String> matchingNames = new HashSet<>();
        Set<String> javaUnmatchedNames = new HashSet<>();
        Set<String> classUnmatchedNames = new HashSet<>();

        int innerClassCnt = 0;
        int javaWithoutClass = 0;
        int classWithoutJava = 0;
        countJavaFiles(srczipPath, javaCounts);
        countClassFiles(rtjarPath, classCounts);

        System.out.println("In .zip: # of .java files in java.io/java.nio packages: " + javaCounts.size());
//        javaCounts.forEach((k, v) -> {
//            System.out.println(k);
//        });

        System.out.println("In .jar: # of .class files in java.io/java.nio packages: " + classCounts.size());
//        classCounts.forEach((k, v) -> {
//            System.out.println(k);
//        });


        for (String key : javaCounts.keySet()) {
            if (classCounts.containsKey(key)) {
                matchingNames.add(key);
            } else {
                javaWithoutClass++;
                javaUnmatchedNames.add(key);
            }
        }

        for (String key : classCounts.keySet()) {
            if (key.contains("$")) {
                innerClassCnt++;
            }
            String formattedClassName = key.replaceAll("\\$.*", "");
            if (!javaCounts.containsKey(formattedClassName)) {
                classWithoutJava++;
                classUnmatchedNames.add(key);
            }
        }


        System.out.println("# of .class files for inner classes: " + innerClassCnt);
        System.out.println("# of .java files with corresponding .class: " + matchingNames.size());
        System.out.println();

        System.out.println("# of .java without its .class: " + javaWithoutClass);
        javaUnmatchedNames.forEach(System.out::println);
        System.out.println();
        System.out.println("# of .class without its .java: " + classWithoutJava);
        classUnmatchedNames.forEach(System.out::println);


    }

    private static void countJavaFiles(String zipFilePath, Map<String, Integer> counts) {
        try (ZipFile zipFile = new ZipFile(zipFilePath)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".java") && (entry.getName().startsWith("java/io/") || entry.getName().startsWith("java/nio/"))) {
                    String packageName = entry.getName().replace('/', '.');
                    packageName = packageName.substring(0, packageName.length() - 5); // Remove ".java"
                    counts.put(packageName, counts.getOrDefault(packageName, 0) + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void countClassFiles(String jarFilePath, Map<String, Integer> counts) {
        try (JarFile jarFile = new JarFile(jarFilePath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && (entry.getName().startsWith("java/io/") || entry.getName().startsWith("java/nio/"))) {
                    String className = entry.getName().replace('/', '.');
                    className = className.substring(0, className.length() - 6); // Remove ".class"
                    counts.put(className, counts.getOrDefault(className, 0) + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int countInnerClassFiles(Map<String, Integer> counts) {
        int innerClassCount = 0;
        for (String key : counts.keySet()) {
            if (key.contains("$")) {
                innerClassCount++;
            }
        }
        return innerClassCount;
    }


}
