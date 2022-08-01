package scripts.api.utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Utility {

    public static <T> List<T> asList(Collection<T> collection) {
        return new ArrayList<>(collection);
    }

    public static List<Integer> intArrayToList(int[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toList());
    }

    public static int[] integerListToIntArray(List<Integer> list) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }

}
