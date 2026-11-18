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

    private static final List<String> EURO_RO_TAILS = List.of("부터", "써", "서", "도", "는", "만", "의");
    private static final List<String> GWA_WA_TAILS = List.of("도", "는", "의", "만");

    /** 자리 바로 뒤에서 알아보는 조사, 고르는 법, 뒤에 이어져도 되는 조사. {@code 으로} 를 {@code 로} 보다 먼저 본다 */
    private static final List<Particle> PARTICLES = List.of(
            new Particle("으로", Josa::euroRo, EURO_RO_TAILS), new Particle("로", Josa::euroRo, EURO_RO_TAILS),
            new Particle("이", Josa::iGa, List.of()), new Particle("가", Josa::iGa, List.of()),
            new Particle("을", Josa::eulReul, List.of()), new Particle("를", Josa::eulReul, List.of()),
            new Particle("은", Josa::eunNeun, List.of()), new Particle("는", Josa::eunNeun, List.of()),
            new Particle("과", Josa::gwaWa, GWA_WA_TAILS), new Particle("와", Josa::gwaWa, GWA_WA_TAILS));

    private EffectLine() {
    }

    /**
     * 문장의 {@code {대상}} 을 대상 이름으로, {@code {시전자}} 를 쓴 캐릭터 이름으로 바꾼다. 바꾼 자리 바로 뒤가 조사 글자
     * ({@code 으로}·{@code 로}·{@code 이}·{@code 가}·{@code 을}·{@code 를}·{@code 은}·{@code 는}·{@code 과}·{@code 와})로
     * 시작하고 그 다음이 경계(문장 끝, 또는 글자·숫자가 아닌 글자 — 공백·문장 부호)이면 조사로 보고 넣은 이름에 맞춰 고른다.
     * 조사 글자 다음에 그 조사의 허용 꼬리({@code 으로/로} 뒤 {@code 부터}·{@code 써}·{@code 서}·{@code 도}·{@code 는}·
     * {@code 만}·{@code 의}, {@code 과/와} 뒤 {@code 도}·{@code 는}·{@code 의}·{@code 만})가 오고 그 다음이 경계여도 조사로 보며,
     * 앞 조사만 고르고 꼬리는 그대로 붙인다({@code {대상}로부터} → {@code 청모랑으로부터}). 그 밖({@code {대상}가슴을}·
     * {@code {대상}이나})은 낱말의 일부로 보고 고치지 않는다.
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
                    if (isParticle(line, i + particle.text().length(), particle.tails())) {
                        result.append(particle.choose().apply(name));
                        i += particle.text().length();
                    }
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

    /** 조사 글자가 끝난 자리 {@code end} 가 경계이거나, 허용 꼬리 하나가 이어진 뒤 경계이면 조사다 */
    private static boolean isParticle(String line, int end, List<String> tails) {
        if (isBoundary(line, end)) return true;
        for (String tail : tails) {
            if (line.startsWith(tail, end) && isBoundary(line, end + tail.length())) return true;
        }
        return false;
    }

    /** 문장 끝이거나 글자·숫자가 아닌 글자(공백·문장 부호) */
    private static boolean isBoundary(String line, int index) {
        return index >= line.length() || !Character.isLetterOrDigit(line.charAt(index));
    }

    private record Particle(String text, UnaryOperator<String> choose, List<String> tails) {
    }
}
