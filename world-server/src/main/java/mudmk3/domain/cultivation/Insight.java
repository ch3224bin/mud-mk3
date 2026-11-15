package mudmk3.domain.cultivation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** 캐릭터의 깨달음: 여섯 갈래 모두 채운다(쌓인 것이 없으면 0). 양은 소수 그대로 */
public record Insight(Map<InsightKind, Double> amounts) {

    public Insight {
        EnumMap<InsightKind, Double> filled = new EnumMap<>(InsightKind.class);
        for (InsightKind kind : InsightKind.values()) {
            filled.put(kind, amounts.getOrDefault(kind, 0.0));
        }
        amounts = Collections.unmodifiableMap(filled);
    }

    public static Insight empty() {
        return new Insight(Map.of());
    }

    public double amount(InsightKind kind) {
        return amounts.get(kind);
    }

    /** 소수점을 버린 양(보일 때) */
    public int floor(InsightKind kind) {
        return (int) Math.floor(amount(kind));
    }
}
