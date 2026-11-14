package mudmk3.domain.npc;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import mudmk3.domain.AbstractEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * NPC: 2차에서는 모두 유니크 NPC다. 인사말·낱말 대답·기본 대답으로 말하고, 방 하나에 배치되거나 배치되지 않는다.
 * 방은 id로만 갖는다(방을 지우면 배치만 풀리므로 FK를 두지 않는다).
 */
@Entity
@Getter
@ToString(callSuper = true, exclude = "topics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Npc extends AbstractEntity {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\S{1,12}$");

    private String name;

    private String greeting;

    private String defaultAnswer;

    private boolean llmEnabled;

    @Getter(AccessLevel.NONE)
    private @Nullable Long roomId;

    @Getter(AccessLevel.NONE)
    @ElementCollection
    private List<NpcTopic> topics = new ArrayList<>();

    public static Npc create(NpcSaveInfo info) {
        Npc npc = new Npc();
        npc.apply(info);
        return npc;
    }

    /** 이름·대사·LLM 켬을 바꾸고 낱말 목록을 통째로 바꾼다. 배치는 그대로다 */
    public void update(NpcSaveInfo info) {
        apply(info);
    }

    public void placeIn(Long roomId) {
        this.roomId = requireNonNull(roomId);
    }

    public void unplace() {
        this.roomId = null;
    }

    public Optional<Long> findRoomId() {
        return Optional.ofNullable(roomId);
    }

    /** 입력 순서의 낱말 목록(바꿀 수 없음) */
    public List<NpcTopic> getTopics() {
        return Collections.unmodifiableList(topics);
    }

    public Optional<NpcTopic> findTopic(String word) {
        return topics.stream()
                .filter(topic -> topic.matches(word))
                .findFirst();
    }

    /** 맞는 낱말의 대답. 없으면 기본 대답 */
    public String answerTo(String word) {
        return findTopic(word).map(NpcTopic::answer).orElse(defaultAnswer);
    }

    /** 이름 전체가 같은지(영문 대소문자 무시) */
    public boolean isCalled(String input) {
        return name.equalsIgnoreCase(input);
    }

    /** 이름이 입력으로 시작하는지(영문 대소문자 무시) */
    public boolean nameStartsWith(String input) {
        return name.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT));
    }

    private void apply(NpcSaveInfo info) {
        requireNonNull(info);
        String newName = requireNonNull(info.name()).strip();
        if (!NAME_PATTERN.matcher(newName).matches()) {
            throw new IllegalArgumentException("NPC 이름은 띄어쓰기 없이 1~12자로 정하십시오: " + newName);
        }
        List<NpcTopic> newTopics = List.copyOf(requireNonNull(info.topics()));
        ensureNoDuplicateWord(newTopics);

        this.name = newName;
        this.greeting = requireNonNull(info.greeting()).strip();
        this.defaultAnswer = requireNonNull(info.defaultAnswer()).strip();
        this.llmEnabled = info.llmEnabled();
        this.topics.clear();
        this.topics.addAll(newTopics);
    }

    private static void ensureNoDuplicateWord(List<NpcTopic> topics) {
        Set<String> seen = new HashSet<>();
        for (NpcTopic topic : topics) {
            if (!seen.add(topic.word().toLowerCase(Locale.ROOT))) {
                throw new IllegalArgumentException("같은 낱말이 두 번 있습니다: " + topic.word());
            }
        }
    }
}
