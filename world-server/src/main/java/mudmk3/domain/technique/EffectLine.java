package mudmk3.domain.technique;

import mudmk3.domain.shared.Josa;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

/** 신통 이펙트 문장의 자리표시: {@code {대상}}(대상 이름)과 {@code {시전자}}(쓴 캐릭터 이름) */
public final class EffectLine {

    public static final String TARGET = "{대상}";
    public static final String CASTER = "{시전자}";

    /** 자리 바로 뒤에서 알아보는 조사와 고르는 법. {@code 으로} 를 {@code 로} 보다 먼저 본다 */
    private static final List<Particle> PARTICLES = List.of(
            new Particle("으로", Josa::euroRo), new Particle("로", Josa::euroRo),
            new Particle("이", Josa::iGa), new Particle("가", Josa::iGa),
            new Particle("을", Josa::eulReul), new Particle("를", Josa::eulReul),
            new Particle("은", Josa::eunNeun), new Particle("는", Josa::eunNeun),
            new Particle("과", Josa::gwaWa), new Particle("와", Josa::gwaWa));

    private EffectLine() {
    }

    /**
     * 문장의 {@code {대상}} 을 대상 이름으로, {@code {시전자}} 를 쓴 캐릭터 이름으로 바꾼다. 바꾼 자리 바로 뒤가 조사
     * ({@code 으로}·{@code 로}·{@code 이}·{@code 가}·{@code 을}·{@code 를}·{@code 은}·{@code 는}·{@code 과}·{@code 와})로
     * 시작하면 그 조사만 넣은 이름에 맞춰 고른다({@code {대상}로부터} → {@code 청모랑으로부터}).
     * {@code targetName} 이 null 이면 {@code {대상}} 은 그대로 둔다
     */
    public static String render(String line, @Nullable String targetName, String casterName) {
        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            String name = null;
            int tokenLength = 0;
            if (line.startsWith(TARGET, i) && targetName != null) {
                name = targetName;
                tokenLength = TARGET.length();
            } else if (line.startsWith(CASTER, i)) {
                name = casterName;
                tokenLength = CASTER.length();
            }
            if (name == null) {
                result.append(line.charAt(i));
                i++;
                continue;
            }
            result.append(name);
            i += tokenLength;
            for (Particle particle : PARTICLES) {
                if (line.startsWith(particle.text(), i)) {
                    result.append(particle.choose().apply(name));
                    i += particle.text().length();
                    break;
                }
            }
        }
        return result.toString();
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

    private record Particle(String text, UnaryOperator<String> choose) {
    }
}
