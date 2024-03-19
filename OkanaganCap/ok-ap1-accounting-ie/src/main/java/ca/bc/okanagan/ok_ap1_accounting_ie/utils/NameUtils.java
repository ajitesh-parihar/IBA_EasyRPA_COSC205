package ca.bc.okanagan.ok_ap1_accounting_ie.utils;

public class NameUtils {

    public static String toSnakeCase(String name) {
        return name.trim().replaceAll("[^\\w_]+", "_")
                .replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    public static String toScreamingSnakeCase(String name) {
        return name.trim().replaceAll("[^\\w_]+", "_")
                .replaceAll("([a-z])([A-Z]+)", "$1_$2").toUpperCase();
    }

    public static String toKebabCase(String name) {
        return name.trim().replaceAll("[^\\w-]+", "-")
                .replaceAll("([a-z])([A-Z]+)", "$1-$2").toLowerCase();
    }
}
