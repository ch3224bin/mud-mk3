package mudmk3.domain.game;

import mudmk3.domain.room.Direction;

import java.util.Optional;

/**
 * 입력 한 줄을 명령으로 바꾼다. 앞뒤 공백을 자르고 여러 공백은 하나로 본다.
 * 명령어는 정해진 이름과 줄임말로만 알아듣는다(앞부분 일치 없음).
 */
public final class CommandParser {

    private static final String SAY_PREFIX = "'";

    private CommandParser() {
    }

    public static Command parse(String line) {
        String normalized = line.strip().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) return new Command.Empty();
        if (normalized.startsWith(SAY_PREFIX)) {
            return new Command.Say(normalized.substring(SAY_PREFIX.length()).strip());
        }

        int space = normalized.indexOf(' ');
        String word = space < 0 ? normalized : normalized.substring(0, space);
        String rest = space < 0 ? "" : normalized.substring(space + 1);

        return switch (word) {
            case "보기", "ㅂ" -> new Command.Look(rest.isEmpty() ? null : rest);
            case "말하기" -> new Command.Say(rest);
            case "누구" -> rest.isEmpty() ? new Command.Who() : new Command.Unknown(word);
            case "도움말" -> rest.isEmpty() ? new Command.Help() : new Command.Unknown(word);
            case "종료" -> rest.isEmpty() ? new Command.Quit() : new Command.Unknown(word);
            default -> parseMove(word, rest);
        };
    }

    private static Command parseMove(String word, String rest) {
        Optional<Direction> direction = Direction.fromCommand(word);
        if (direction.isPresent() && rest.isEmpty()) {
            return new Command.Move(direction.get());
        }
        return new Command.Unknown(word);
    }
}
