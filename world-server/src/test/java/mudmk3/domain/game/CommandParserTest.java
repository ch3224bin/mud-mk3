package mudmk3.domain.game;

import mudmk3.domain.game.Command.Empty;
import mudmk3.domain.game.Command.Help;
import mudmk3.domain.game.Command.Look;
import mudmk3.domain.game.Command.Move;
import mudmk3.domain.game.Command.Quit;
import mudmk3.domain.game.Command.Say;
import mudmk3.domain.game.Command.Unknown;
import mudmk3.domain.game.Command.Who;
import mudmk3.domain.room.Direction;
import org.junit.jupiter.api.Test;

import static mudmk3.domain.game.CommandParser.parse;
import static org.assertj.core.api.Assertions.assertThat;

class CommandParserTest {

    @Test
    void move() {
        assertThat(parse("  북  ")).isEqualTo(new Move(Direction.NORTH));
        assertThat(parse("NE")).isEqualTo(new Move(Direction.NORTHEAST));
        assertThat(parse("아래")).isEqualTo(new Move(Direction.DOWN));
        assertThat(parse("u")).isEqualTo(new Move(Direction.UP));
    }

    @Test
    void look() {
        assertThat(parse("ㅂ")).isEqualTo(new Look(null));
        assertThat(parse("보기")).isEqualTo(new Look(null));
        assertThat(parse("보기 한립")).isEqualTo(new Look("한립"));
        assertThat(parse("보기   늙은   소나무 ")).isEqualTo(new Look("늙은 소나무"));
    }

    @Test
    void say() {
        assertThat(parse("'안녕하시오")).isEqualTo(new Say("안녕하시오"));
        assertThat(parse("' 안녕하시오 ")).isEqualTo(new Say("안녕하시오"));
        assertThat(parse("말하기   길을   묻겠소")).isEqualTo(new Say("길을 묻겠소"));
        assertThat(parse("말하기")).isEqualTo(new Say(""));
        assertThat(parse("'")).isEqualTo(new Say(""));
    }

    @Test
    void others() {
        assertThat(parse("누구")).isEqualTo(new Who());
        assertThat(parse("도움말")).isEqualTo(new Help());
        assertThat(parse("종료")).isEqualTo(new Quit());
        assertThat(parse("")).isEqualTo(new Empty());
        assertThat(parse("   ")).isEqualTo(new Empty());
    }

    @Test
    void unknownFail() {
        assertThat(parse("말 안녕")).isEqualTo(new Unknown("말"));
        assertThat(parse("보")).isEqualTo(new Unknown("보"));
        assertThat(parse("북 가자")).isEqualTo(new Unknown("북"));
    }
}
