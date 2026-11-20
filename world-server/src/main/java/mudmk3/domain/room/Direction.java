package mudmk3.domain.room;

import java.util.Arrays;
import java.util.Optional;

/** 출구 방위. 선언 순서가 출구를 보여 주는 순서다 */
public enum Direction {
    EAST("동", "e", "ㄷ"),
    WEST("서", "w", "ㅅ"),
    SOUTH("남", "s", "ㄴ"),
    NORTH("북", "n", "ㅂ"),
    NORTHEAST("북동", "ne", "ㅂㄷ"),
    NORTHWEST("북서", "nw", "ㅂㅅ"),
    SOUTHEAST("남동", "se", "ㄴㄷ"),
    SOUTHWEST("남서", "sw", "ㄴㅅ"),
    UP("위", "u", "ㅇ"),
    DOWN("아래", "d", "ㅇㄹ");

    private final String koreanName;
    private final String abbreviation;
    private final String initials;

    Direction(String koreanName, String abbreviation, String initials) {
        this.koreanName = koreanName;
        this.abbreviation = abbreviation;
        this.initials = initials;
    }

    public String koreanName() {
        return koreanName;
    }

    public String abbreviation() {
        return abbreviation;
    }

    /** 한글 이름의 초성 약자(호환 자모): {@code ㄷ}, {@code ㅂㄷ} */
    public String initials() {
        return initials;
    }

    public Direction opposite() {
        return switch (this) {
            case EAST -> WEST;
            case WEST -> EAST;
            case SOUTH -> NORTH;
            case NORTH -> SOUTH;
            case NORTHEAST -> SOUTHWEST;
            case NORTHWEST -> SOUTHEAST;
            case SOUTHEAST -> NORTHWEST;
            case SOUTHWEST -> NORTHEAST;
            case UP -> DOWN;
            case DOWN -> UP;
        };
    }

    /** 그 방위로 향하는 말: {@code 동쪽으로}, {@code 위로} */
    public String towardPhrase() {
        return isVertical() ? koreanName + "로" : koreanName + "쪽으로";
    }

    /** 그 방위에서 오는 말: {@code 동쪽에서}, {@code 위에서} */
    public String fromPhrase() {
        return isVertical() ? koreanName + "에서" : koreanName + "쪽에서";
    }

    /**
     * 방위 낱말(한글 이름, 영문 줄임말(대소문자 무시), 초성 약자)에 맞는 방위. 이동 명령과 방위를 인자로 받는 명령이 함께 쓴다
     */
    public static Optional<Direction> fromCommand(String word) {
        return Arrays.stream(values())
                .filter(direction -> direction.koreanName.equals(word)
                        || direction.abbreviation.equalsIgnoreCase(word)
                        || direction.initials.equals(word))
                .findFirst();
    }

    private boolean isVertical() {
        return this == UP || this == DOWN;
    }
}
