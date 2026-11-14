package mudmk3.domain.game;

import mudmk3.domain.game.Command.Empty;
import mudmk3.domain.game.Command.Drop;
import mudmk3.domain.game.Command.Help;
import mudmk3.domain.game.Command.Inventory;
import mudmk3.domain.game.Command.Look;
import mudmk3.domain.game.Command.Move;
import mudmk3.domain.game.Command.Pick;
import mudmk3.domain.game.Command.Quit;
import mudmk3.domain.game.Command.Say;
import mudmk3.domain.game.Command.Talk;
import mudmk3.domain.game.Command.Unknown;
import mudmk3.domain.game.Command.Who;
import mudmk3.domain.item.ItemQuery;
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
    void talk() {
        assertThat(parse("대화")).isEqualTo(new Talk(null, null));
        assertThat(parse("대화 왕씨")).isEqualTo(new Talk("왕씨", null));
        assertThat(parse("대화 왕씨 일거리")).isEqualTo(new Talk("왕씨", "일거리"));
        assertThat(parse("대화   왕씨   [일거리]")).isEqualTo(new Talk("왕씨", "일거리"));
        assertThat(parse("대화 왕씨 늑대 퇴치")).isEqualTo(new Talk("왕씨", "늑대 퇴치"));
        assertThat(parse("대화 왕씨 []")).isEqualTo(new Talk("왕씨", null));
    }

    @Test
    void talkWithoutSpaceFail() {
        assertThat(parse("대화왕씨")).isEqualTo(new Unknown("대화왕씨"));
        assertThat(parse("말 왕씨")).isEqualTo(new Unknown("말"));
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

    @Test
    void inventory() {
        assertThat(parse("소지품")).isEqualTo(new Inventory());
        assertThat(parse("ㅈ")).isEqualTo(new Inventory());
        assertThat(parse(" ㅈ ")).isEqualTo(new Inventory());
    }

    @Test
    void inventoryWithArgumentFail() {
        assertThat(parse("소지품 x")).isEqualTo(new Unknown("소지품"));
        assertThat(parse("ㅈ 가죽")).isEqualTo(new Unknown("ㅈ"));
    }

    @Test
    void pick() {
        assertThat(parse("줍기")).isEqualTo(new Pick(new ItemArguments(null, Amount.one())));
        assertThat(parse("줍기 2.가죽")).isEqualTo(new Pick(new ItemArguments(new ItemQuery("가죽", 2), Amount.one())));
        assertThat(parse("줍기 늑대 가죽 전부")).isEqualTo(new Pick(new ItemArguments(new ItemQuery("늑대 가죽", 0), new Amount.All())));
    }

    @Test
    void drop() {
        assertThat(parse("버리기 하품 10")).isEqualTo(new Drop(new ItemArguments(new ItemQuery("하품 영석", 0), new Amount.Count(10))));
        assertThat(parse("버리기")).isEqualTo(new Drop(new ItemArguments(null, Amount.one())));
    }
}
