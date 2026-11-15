package mudmk3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.random.RandomGenerator;

/**
 * 테스트가 정한 값을 차례로 내는 난수. 넣은 값이 다 떨어지면 {@code nextDouble()} 은 0.999, {@code nextInt(bound)} 는 0 이다
 * (영석·계층 드롭이 나오지 않는 쪽). 그 밖의 난수는 쓰지 않으므로 {@code nextLong()} 은 막는다.
 * {@code @ApplicationServiceTest} 는 테스트마다 {@link #reset} 으로 비운다
 */
public class ScriptedRandom implements RandomGenerator {

    public static final double DEFAULT_DOUBLE = 0.999;

    private final Object lock = new Object();
    private final Deque<Double> doubles = new ArrayDeque<>();
    private final Deque<Integer> ints = new ArrayDeque<>();
    private int doubleCalls;
    private int intCalls;

    public ScriptedRandom enqueueDoubles(double... values) {
        synchronized (lock) {
            for (double value : values) {
                if (value < 0 || value >= 1) {
                    throw new IllegalArgumentException("nextDouble 값은 0 이상 1 미만이어야 합니다: " + value);
                }
                doubles.addLast(value);
            }
            return this;
        }
    }

    public ScriptedRandom enqueueInts(int... values) {
        synchronized (lock) {
            for (int value : values) {
                ints.addLast(value);
            }
            return this;
        }
    }

    @Override
    public double nextDouble() {
        synchronized (lock) {
            doubleCalls++;
            Double value = doubles.pollFirst();
            return value == null ? DEFAULT_DOUBLE : value;
        }
    }

    @Override
    public int nextInt(int bound) {
        synchronized (lock) {
            intCalls++;
            Integer value = ints.pollFirst();
            int result = value == null ? 0 : value;
            if (result < 0 || result >= bound) {
                throw new IllegalStateException("nextInt(" + bound + ") 에 넣은 값이 범위를 벗어납니다: " + result);
            }
            return result;
        }
    }

    @Override
    public long nextLong() {
        throw new UnsupportedOperationException("ScriptedRandom 은 nextDouble·nextInt(bound) 만 냅니다");
    }

    /** 지금까지 부른 {@code nextDouble()} 수 */
    public int doubleCalls() {
        synchronized (lock) {
            return doubleCalls;
        }
    }

    /** 지금까지 부른 {@code nextInt(bound)} 수 */
    public int intCalls() {
        synchronized (lock) {
            return intCalls;
        }
    }

    /** 넣은 값과 부른 수를 비운다 */
    public void reset() {
        synchronized (lock) {
            doubles.clear();
            ints.clear();
            doubleCalls = 0;
            intCalls = 0;
        }
    }
}
