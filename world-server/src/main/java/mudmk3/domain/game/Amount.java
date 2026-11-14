package mudmk3.domain.game;

/** 물건 명령의 수량: 몇 개, {@code 전부}, 잘못 적은 수(0이나 10자리 이상) */
public sealed interface Amount {

    /** 수량으로 적을 수 있는 가장 큰 수(9자리, D46) */
    int MAX_COUNT = 999_999_999;

    record Count(int value) implements Amount {
        public Count {
            if (value < 1 || value > MAX_COUNT) {
                throw new IllegalArgumentException("수량은 1 이상 999,999,999 이하여야 합니다: " + value);
            }
        }
    }

    record All() implements Amount {
    }

    record Invalid() implements Amount {
    }

    /** 수량을 빼면 1개 */
    static Amount one() {
        return new Count(1);
    }
}
