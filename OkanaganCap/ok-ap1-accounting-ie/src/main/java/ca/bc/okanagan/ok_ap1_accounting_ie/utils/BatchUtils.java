package ca.bc.okanagan.ok_ap1_accounting_ie.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BatchUtils {

    public static <T> List<List<T>> splitIntoBatches(List<T> list, int threadsAmount) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < threadsAmount; i++) {
            batches.add(new ArrayList<>());
        }
        int j = 0;
        while (j < list.size()) {
            for (int i = 0; i < threadsAmount; i++) {
                if (j < list.size()) {
                    batches.get(i).add(list.get(j++));
                } else {
                    break;
                }
            }
        }
        return batches.stream().filter(l -> l.size() > 0).collect(Collectors.toList());
    }

}
