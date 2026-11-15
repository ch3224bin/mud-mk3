package mudmk3.domain.technique;

import java.util.Optional;

/** 신통 이펙트 문장의 자리표시: {@code {대상}}(대상 이름)과 {@code {시전자}}(쓴 캐릭터 이름) */
public final class EffectLine {

    public static final String TARGET = "{대상}";
    public static final String CASTER = "{시전자}";

    private EffectLine() {
    }

    /**
     * 앞에서부터 훑어 처음 걸린 자리표시 토큰을 돌려준다. {@code {} 를 만나면 그 뒤 첫 {@code }} 까지(포함)를 토큰으로 보고
     * {@code {대상}}·{@code {시전자}} 가 아니면 그 토큰이, 뒤에 {@code }} 가 없으면 {@code {} 하나가 걸린다.
     * {@code {} 없이 나온 {@code }} 는 {@code }} 하나가 걸린다. 걸린 것이 없으면 빈 값
     */
    public static Optional<String> firstBadToken(String line) {
        int i = 0;
        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '}') return Optional.of("}");
            if (c == '{') {
                int close = line.indexOf('}', i + 1);
                if (close < 0) return Optional.of("{");
                String token = line.substring(i, close + 1);
                if (!token.equals(TARGET) && !token.equals(CASTER)) return Optional.of(token);
                i = close + 1;
                continue;
            }
            i++;
        }
        return Optional.empty();
    }
}
