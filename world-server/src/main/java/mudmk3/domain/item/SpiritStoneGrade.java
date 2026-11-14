package mudmk3.domain.item;

import java.util.Arrays;
import java.util.Optional;

/** 영석 등급. 선언 순서가 하품 → 극품이고, 값은 하품 기준이다 */
public enum SpiritStoneGrade {
    LOW("하품", 1, "영기가 옅게 서린 돌. 수선자들이 값을 치르는 데 쓴다."),
    MIDDLE("중품", 100, "영기가 또렷하게 서린 돌. 하품 영석 100개의 값이다."),
    HIGH("상품", 10_000, "영기가 짙게 서린 돌. 하품 영석 10,000개의 값이다."),
    TOP("극품", 1_000_000, "영기가 넘실대는 돌. 하품 영석 1,000,000개의 값이다.");

    private final String koreanName;
    private final long value;
    private final String description;

    SpiritStoneGrade(String koreanName, long value, String description) {
        this.koreanName = koreanName;
        this.value = value;
        this.description = description;
    }

    public String koreanName() {
        return koreanName;
    }

    /** 하품 영석으로 친 값 */
    public long value() {
        return value;
    }

    /** 이 등급 영석의 물건 이름: {@code 하품 영석} */
    public String itemName() {
        return koreanName + " 영석";
    }

    /** 이 등급 영석의 묘사 */
    public String description() {
        return description;
    }

    /** 등급 낱말 하나({@code 하품} 등)에 맞는 등급 */
    public static Optional<SpiritStoneGrade> fromWord(String word) {
        return Arrays.stream(values())
                .filter(grade -> grade.koreanName.equals(word))
                .findFirst();
    }
}
