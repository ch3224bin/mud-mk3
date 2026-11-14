package mudmk3.domain.game;

import mudmk3.domain.room.Direction;
import mudmk3.domain.shared.Names;

import java.util.Optional;

/**
 * 입력 한 줄을 명령으로 바꾼다. 앞뒤 공백을 자르고 여러 공백은 하나로 본다.
 * 명령어는 정해진 이름과 줄임말로만 알아듣는다(앞부분 일치 없음).
 */
public final class CommandParser {

    private static final String SAY_PREFIX = "'";
    private static final String UNBIND_WORD = "해제";

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
            case "대화" -> parseTalk(rest);
            case "소지품", "ㅈ" -> rest.isEmpty() ? new Command.Inventory() : new Command.Unknown(word);
            case "줍기" -> new Command.Pick(ItemArguments.parse(rest));
            case "버리기" -> new Command.Drop(ItemArguments.parse(rest));
            case "목록" -> new Command.ShopList(rest);
            case "사기" -> new Command.Buy(rest);
            case "팔기" -> new Command.Sell(rest);
            case "값" -> new Command.Appraise(rest);
            case "주기" -> parseGive(rest);
            case "귀속" -> parseBind(rest);
            case "확인" -> rest.isEmpty() ? new Command.Confirm() : new Command.Unknown(word);
            case "취소" -> rest.isEmpty() ? new Command.Cancel() : new Command.Unknown(word);
            case "누구" -> rest.isEmpty() ? new Command.Who() : new Command.Unknown(word);
            case "도움말" -> rest.isEmpty() ? new Command.Help() : new Command.Unknown(word);
            case "종료" -> rest.isEmpty() ? new Command.Quit() : new Command.Unknown(word);
            default -> parseMove(word, rest);
        };
    }

    /** 첫 낱말이 대상, 나머지 전부가 낱말(앞뒤 [ ] 를 뗌). 비면 null */
    private static Command parseTalk(String rest) {
        if (rest.isEmpty()) return new Command.Talk(null, null);
        int space = rest.indexOf(' ');
        if (space < 0) return new Command.Talk(rest, null);
        String word = Names.stripBrackets(rest.substring(space + 1));
        return new Command.Talk(rest.substring(0, space), word.isEmpty() ? null : word);
    }

    /** 첫 낱말이 대상, 나머지가 물건 인자. 대상이 없으면 null */
    private static Command parseGive(String rest) {
        if (rest.isEmpty()) return new Command.Give(null, "");
        int space = rest.indexOf(' ');
        if (space < 0) return new Command.Give(rest, "");
        return new Command.Give(rest.substring(0, space), rest.substring(space + 1));
    }

    /** {@code 귀속 해제 <물건>} 은 귀속 해제, 그 밖은 {@code 귀속 <물건>} */
    private static Command parseBind(String rest) {
        if (rest.equals(UNBIND_WORD)) return new Command.Unbind("");
        if (rest.startsWith(UNBIND_WORD + " ")) return new Command.Unbind(rest.substring(UNBIND_WORD.length() + 1));
        return new Command.Bind(rest);
    }

    private static Command parseMove(String word, String rest) {
        Optional<Direction> direction = Direction.fromCommand(word);
        if (direction.isPresent() && rest.isEmpty()) {
            return new Command.Move(direction.get());
        }
        return new Command.Unknown(word);
    }
}
