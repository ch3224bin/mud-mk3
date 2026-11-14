package mudmk3.domain.game;

import mudmk3.domain.room.Direction;
import org.jspecify.annotations.Nullable;

import static java.util.Objects.requireNonNull;

/** 플레이어가 입력한 한 줄을 알아들은 명령 */
public sealed interface Command {

    /** 지금 방 보기. 인자가 있으면 그 대상 보기(3차엔 물건만 자세히 보인다) */
    record Look(@Nullable String argument) implements Command {
    }

    record Move(Direction direction) implements Command {
        public Move {
            requireNonNull(direction);
        }
    }

    /** 같은 방에 말하기. 내용이 빠졌으면 빈 문자열 */
    record Say(String message) implements Command {
        public Say {
            requireNonNull(message);
        }
    }

    /** NPC에게 말 걸기. 대상이 빠졌으면 null, 낱말이 빠졌으면 null(인사) */
    record Talk(@Nullable String target, @Nullable String word) implements Command {
    }

    /** 소지품 보기 */
    record Inventory() implements Command {
    }

    /** 바닥의 물건 줍기 */
    record Pick(ItemArguments args) implements Command {
        public Pick {
            requireNonNull(args);
        }
    }

    /** 소지품의 물건 버리기 */
    record Drop(ItemArguments args) implements Command {
        public Drop {
            requireNonNull(args);
        }
    }

    record Who() implements Command {
    }

    record Help() implements Command {
    }

    record Quit() implements Command {
    }

    /** 빈 줄 */
    record Empty() implements Command {
    }

    /** 알 수 없는 명령. 첫 낱말을 갖는다 */
    record Unknown(String word) implements Command {
        public Unknown {
            requireNonNull(word);
        }
    }
}
