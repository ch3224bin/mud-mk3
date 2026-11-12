package mudmk3.adapter.websocket;

import org.jspecify.annotations.Nullable;

/** 클라이언트가 보내는 메시지. 1차엔 {@code {"type":"command","text":"..."}} 만 있다 */
public record InboundMessage(@Nullable String type, @Nullable String text) {

    public static final String COMMAND = "command";

    /** 입력 한 줄(type이 command)이면 true */
    public boolean isCommand() {
        return COMMAND.equals(type);
    }
}
