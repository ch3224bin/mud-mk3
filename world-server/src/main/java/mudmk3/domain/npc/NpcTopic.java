package mudmk3.domain.npc;

import mudmk3.domain.shared.Names;

import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/** NPC가 알아듣는 낱말과 그 대답. 낱말은 앞뒤 공백과 앞뒤 [ ] 를 뗀 값으로 저장한다 */
public record NpcTopic(String word, String answer) {

    public static final int ANSWER_MAX_LENGTH = 1000;

    private static final Pattern WORD_PATTERN = Pattern.compile("^\\S{1,20}$");

    public NpcTopic {
        word = Names.stripBrackets(requireNonNull(word));
        if (!WORD_PATTERN.matcher(word).matches()) {
            throw new IllegalArgumentException("낱말은 띄어쓰기 없이 1~20자로 정하십시오: " + word);
        }
        requireNonNull(answer);
        if (answer.isBlank() || answer.length() > ANSWER_MAX_LENGTH) {
            throw new IllegalArgumentException("대답은 1~1000자로 정하십시오: " + word);
        }
    }

    /** 입력을 같은 방식으로 다듬어 낱말과 글자 그대로 같은지 본다(영문 대소문자 무시, 앞부분 일치 없음) */
    public boolean matches(String input) {
        return word.equalsIgnoreCase(Names.stripBrackets(input));
    }
}
