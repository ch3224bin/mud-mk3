package mudmk3.application.game.required;

import org.jspecify.annotations.Nullable;

import java.util.List;

import static java.util.Objects.requireNonNull;

/** 서버가 플레이어에게 보내는 메시지. record 필드 이름이 JSON 필드 이름이고 {@link #type()} 이 JSON {@code type} 이다 */
public sealed interface GameMessage {

    String type();

    /** 접속 상태가 바뀜. state: {@code creating} | {@code playing} */
    record SessionMessage(String state, @Nullable String characterName) implements GameMessage {
        public static final String CREATING = "creating";
        public static final String PLAYING = "playing";

        @Override
        public String type() {
            return "session";
        }
    }

    /** 캐릭터 생성 중 묻는 말 */
    record PromptMessage(String text) implements GameMessage {
        @Override
        public String type() {
            return "prompt";
        }
    }

    /**
     * 방 보기. owner는 지역을 소유한 성·종문(없으면 null), exits는 방위 한글 이름(방위 순서),
     * occupants는 나를 뺀 사람(들어온 순서) 다음 그 방의 NPC(만든 순서)
     */
    record RoomMessage(
            ZoneView zone,
            boolean zoneEntered,
            RoomView room,
            @Nullable OwnerView owner,
            List<String> exits,
            List<OccupantView> occupants
    ) implements GameMessage {
        public RoomMessage {
            exits = List.copyOf(exits);
            occupants = List.copyOf(occupants);
        }

        @Override
        public String type() {
            return "room";
        }

        public record ZoneView(Long id, String name) {
        }

        public record RoomView(Long id, String name, String description) {
        }

        /** kind: {@code castle} | {@code sect} */
        public record OwnerView(String kind, String name) {
        }

        /** kind: {@code player} | {@code npc} */
        public record OccupantView(String name, String kind) {
            public static OccupantView player(String name) {
                return new OccupantView(name, "player");
            }

            public static OccupantView npc(String name) {
                return new OccupantView(name, "npc");
            }
        }
    }

    /** 안내·결과·거절. tone: {@code info} | {@code error} */
    record SystemMessage(String text, String tone) implements GameMessage {
        public static SystemMessage info(String text) {
            return new SystemMessage(text, "info");
        }

        public static SystemMessage error(String text) {
            return new SystemMessage(text, "error");
        }

        @Override
        public String type() {
            return "system";
        }
    }

    /** 같은 방에서 일어난 일. event: {@code arrive} | {@code leave} | {@code appear} | {@code disappear} */
    record NoticeMessage(String text, String tone, String actor, String event) implements GameMessage {
        public static NoticeMessage movement(String text, String actor, String event) {
            return new NoticeMessage(requireNonNull(text), "movement", actor, event);
        }

        @Override
        public String type() {
            return "notice";
        }
    }

    /** 말하기. text는 그린 한 줄 */
    record SayMessage(String speaker, String message, boolean self, String text, String tone) implements GameMessage {
        public static SayMessage speech(String speaker, String message, boolean self, String text) {
            return new SayMessage(speaker, message, self, text, "speech");
        }

        @Override
        public String type() {
            return "say";
        }
    }

    /** NPC 대답(묻는 사람에게만). word는 물은 낱말(인사면 null), text는 그린 줄들({@code \n} 으로 나뉨) */
    record TalkMessage(String npc, @Nullable String word, String text, String tone) implements GameMessage {
        public static TalkMessage talk(String npc, @Nullable String word, String text) {
            return new TalkMessage(npc, word, text, "talk");
        }

        @Override
        public String type() {
            return "talk";
        }
    }
}
