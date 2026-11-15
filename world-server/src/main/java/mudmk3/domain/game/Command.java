package mudmk3.domain.game;

import mudmk3.domain.item.ItemQuery;
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

    /** 상점 목록. 인자는 상점 NPC 이름(없으면 빈 문자열) */
    record ShopList(String args) implements Command {
        public ShopList {
            requireNonNull(args);
        }
    }

    /** 상점에서 사기. 인자는 다듬기 전 나머지 전부(앞의 NPC 이름은 방의 상점 수를 아는 처리기가 뗀다) */
    record Buy(String args) implements Command {
        public Buy {
            requireNonNull(args);
        }
    }

    /** 상점에 팔기. 인자는 {@link Buy} 와 같다 */
    record Sell(String args) implements Command {
        public Sell {
            requireNonNull(args);
        }
    }

    /** 상점 매입가 보기. 인자는 {@link Buy} 와 같다 */
    record Appraise(String args) implements Command {
        public Appraise {
            requireNonNull(args);
        }
    }

    /** 같은 방 플레이어에게 주기. 대상이 빠졌으면 null, 물건 인자가 빠졌으면 빈 문자열 */
    record Give(@Nullable String target, String args) implements Command {
        public Give {
            requireNonNull(args);
        }
    }

    /** 묻던 것(상위 등급 영석 깨기 결제) 승인 */
    record Confirm() implements Command {
    }

    /** 묻던 것 그만두기 */
    record Cancel() implements Command {
    }

    /** 경지·영근·스탯·수위·깨달음 보기 */
    record Status() implements Command {
    }

    /** 익힌 공법 보기 */
    record Techniques() implements Command {
    }

    /** 배운 신통 보기 */
    record Abilities() implements Command {
    }

    /** 소지품의 옥간·서책 읽기. 인자 전체가 이름(번호 허용, 수량 없음), 빠졌으면 null */
    record Read(@Nullable ItemQuery item) implements Command {
    }

    /** 같은 방의 몹과 교전을 시작하거나 노리는 몹을 바꾼다. 인자 전체가 이름(번호 허용, 수량 없음), 빠졌으면 null */
    record Attack(@Nullable ItemQuery target) implements Command {
    }

    /** 시체의 물건을 거둔다. 인자 전체가 시체 이름(번호 허용, 수량 없음), 빠졌으면 null */
    record Collect(@Nullable ItemQuery corpse) implements Command {
    }

    /**
     * 신통 쓰기: 첫 낱말이 명령어가 아니면 신통 이름으로 본다. 인자 전체가 대상 이름(번호 허용, 수량 없음), 빠졌으면 null
     */
    record UseAbility(String name, @Nullable ItemQuery target) implements Command {
        public UseAbility {
            requireNonNull(name);
        }
    }

    /** 시전 중인 신통 그만두기 */
    record Stop() implements Command {
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
