package mudmk3.domain.room;

import java.util.Arrays;
import java.util.Optional;

/** 출구 방위. 선언 순서가 출구를 보여 주는 순서다 */
public enum Direction {
    EAST("동", "e"),
    WEST("서", "w"),
    SOUTH("남", "s"),
    NORTH("북", "n"),
    NORTHEAST("북동", "ne"),
    NORTHWEST("북서", "nw"),
    SOUTHEAST("남동", "se"),
    SOUTHWEST("남서", "sw"),
    UP("위", "u"),
    DOWN("아래", "d");

    private final String koreanName;
    private final String abbreviation;

    Direction(String koreanName, String abbreviation) {
        this.koreanName = koreanName;
        this.abbreviation = abbreviation;
    }

    public String koreanName() {
        return koreanName;
    }

    public String abbreviation() {
        return abbreviation;
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

    /** 명령 낱말(한글 이름 또는 줄임말, 줄임말은 대소문자 무시)에 맞는 방위 */
    public static Optional<Direction> fromCommand(String word) {
        return Arrays.stream(values())
                .filter(direction -> direction.koreanName.equals(word)
                        || direction.abbreviation.equalsIgnoreCase(word))
                .findFirst();
    }

    private boolean isVertical() {
        return this == UP || this == DOWN;
    }
}
