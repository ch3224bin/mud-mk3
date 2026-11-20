package mudmk3.domain.game;

import mudmk3.domain.item.ItemNames;
import mudmk3.domain.item.ItemQuery;
import mudmk3.domain.room.Direction;
import mudmk3.domain.shared.Names;
import mudmk3.domain.technique.ReservedWords;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * 입력 한 줄을 명령으로 바꾼다. 앞뒤 공백을 자르고 여러 공백은 하나로 본다.
 * 명령어는 정해진 이름과 줄임말로만 알아듣는다(앞부분 일치 없음). 명령어가 아닌 첫 낱말은 신통 이름으로 본다.
 * 방위 낱말(이름·영문·초성 약자, {@link Direction#fromCommand})은 아래 명령어 목록에 넣지 않고 {@link #parseOther} 에서 읽는다.
 * 그래서 {@code ㅂ}(북)·{@code ㅅ}(서)는 다른 명령 줄임말로 쓰지 않는다(command.md 방위 초성 약자: 겹치면 방위가 먼저).
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
            case "보기", "봐", "보" -> parseLook(rest);
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
            case "확인" -> rest.isEmpty() ? new Command.Confirm() : new Command.Unknown(word);
            case "취소" -> rest.isEmpty() ? new Command.Cancel() : new Command.Unknown(word);
            case "상태", "ㅅㅌ" -> rest.isEmpty() ? new Command.Status() : new Command.Unknown(word);
            case "공법" -> rest.isEmpty() ? new Command.Techniques() : new Command.Unknown(word);
            case "신통" -> rest.isEmpty() ? new Command.Abilities() : new Command.Unknown(word);
            case "읽기" -> new Command.Read(wholeName(rest));
            case "공격" -> new Command.Attack(wholeName(rest));
            case "수습" -> new Command.Collect(wholeName(rest));
            case "누구" -> rest.isEmpty() ? new Command.Who() : new Command.Unknown(word);
            case "도움말" -> rest.isEmpty() ? new Command.Help() : new Command.Unknown(word);
            case "종료" -> rest.isEmpty() ? new Command.Quit() : new Command.Unknown(word);
            case "중지" -> rest.isEmpty() ? new Command.Stop() : new Command.Unknown(word);
            default -> parseOther(word, rest);
        };
    }

    /** 인자 전체가 방위 낱말이면 그 방위 보기(방위가 같은 이름의 대상보다 먼저), 아니면 지금 방이나 대상 보기 */
    private static Command parseLook(String rest) {
        if (rest.isEmpty()) return new Command.Look(null);
        return Direction.fromCommand(rest)
                .<Command>map(Command.LookToward::new)
                .orElseGet(() -> new Command.Look(rest));
    }

    /** 첫 낱말이 대상, 나머지 전부가 낱말(앞뒤 [ ] 를 뗌). 비면 null */
    private static Command parseTalk(String rest) {
        if (rest.isEmpty()) return new Command.Talk(null, null);
        int space = rest.indexOf(' ');
        if (space < 0) return new Command.Talk(rest, null);
        String word = Names.stripBrackets(rest.substring(space + 1));
        return new Command.Talk(rest.substring(0, space), word.isEmpty() ? null : word);
    }

    /** 인자 전체가 이름(번호 허용). 수량은 받지 않는다. 비면 null */
    private static @Nullable ItemQuery wholeName(String rest) {
        String name = ItemNames.normalize(rest);
        return name.isEmpty() ? null : ItemQuery.parse(name);
    }

    /** 첫 낱말이 대상, 나머지가 물건 인자. 대상이 없으면 null */
    private static Command parseGive(String rest) {
        if (rest.isEmpty()) return new Command.Give(null, "");
        int space = rest.indexOf(' ');
        if (space < 0) return new Command.Give(rest, "");
        return new Command.Give(rest.substring(0, space), rest.substring(space + 1));
    }

    /**
     * 방위 명령이면 이동(인자가 있으면 알 수 없음), 아직 없는 명령어(예약어)면 알 수 없음, 그 밖의 첫 낱말은 신통 이름이다
     * (command.md 신통 쓰기 이름 해석 1)
     */
    private static Command parseOther(String word, String rest) {
        Optional<Direction> direction = Direction.fromCommand(word);
        if (direction.isPresent()) {
            return rest.isEmpty() ? new Command.Move(direction.get()) : new Command.Unknown(word);
        }
        if (ReservedWords.contains(word)) {
            return new Command.Unknown(word);
        }
        return new Command.UseAbility(word, wholeName(rest));
    }
}
