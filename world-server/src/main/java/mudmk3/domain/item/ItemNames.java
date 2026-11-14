package mudmk3.domain.item;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/** 물건 이름을 다듬고, 명령의 이름 입력이 물건에 맞는지 보고 고른다(룰북 commands.md 대상 지정) */
public final class ItemNames {

    private static final Pattern SPACES = Pattern.compile("\\s+");

    private ItemNames() {
    }

    /** 앞뒤 공백을 자르고 여러 공백을 하나로 바꾼다({@code "  늑대   가죽 "} → {@code 늑대 가죽}) */
    public static String normalize(String name) {
        return SPACES.matcher(requireNonNull(name).strip()).replaceAll(" ");
    }

    /** 맞는 때: 이름 전체, 이름의 앞부분, 이름을 이루는 낱말 하나의 앞부분(영문 대소문자 무시) */
    public static boolean matches(String itemName, String query) {
        String name = itemName.toLowerCase(Locale.ROOT);
        String input = query.toLowerCase(Locale.ROOT);
        if (input.isBlank()) return false;
        if (name.startsWith(input)) return true;
        for (String word : SPACES.split(name)) {
            if (word.startsWith(input)) return true;
        }
        return false;
    }

    /**
     * 보이는 순서의 후보에서 하나를 고른다. 번호가 없으면 이름 전체가 같은 것을 먼저, 없으면 맞는 것 중 첫째.
     * 번호가 있으면 전체 일치 우선 없이 맞는 것을 보이는 순서로 센다
     */
    public static <T> Optional<T> select(List<T> candidates, Function<T, String> nameOf, ItemQuery query) {
        List<T> matched = candidates.stream()
                .filter(candidate -> matches(nameOf.apply(candidate), query.name()))
                .toList();
        if (query.hasOrdinal()) {
            return query.ordinal() <= matched.size()
                    ? Optional.of(matched.get(query.ordinal() - 1))
                    : Optional.empty();
        }
        return matched.stream()
                .filter(candidate -> nameOf.apply(candidate).equalsIgnoreCase(query.name()))
                .findFirst()
                .or(() -> matched.stream().findFirst());
    }
}
