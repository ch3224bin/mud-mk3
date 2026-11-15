package mudmk3.domain.game;

import mudmk3.domain.game.Command.Abilities;
import mudmk3.domain.game.Command.Appraise;
import mudmk3.domain.game.Command.Buy;
import mudmk3.domain.game.Command.Cancel;
import mudmk3.domain.game.Command.Confirm;
import mudmk3.domain.game.Command.Empty;
import mudmk3.domain.game.Command.Give;
import mudmk3.domain.game.Command.Sell;
import mudmk3.domain.game.Command.ShopList;
import mudmk3.domain.game.Command.Drop;
import mudmk3.domain.game.Command.Help;
import mudmk3.domain.game.Command.Inventory;
import mudmk3.domain.game.Command.Look;
import mudmk3.domain.game.Command.Move;
import mudmk3.domain.game.Command.Pick;
import mudmk3.domain.game.Command.Quit;
import mudmk3.domain.game.Command.Read;
import mudmk3.domain.game.Command.Say;
import mudmk3.domain.game.Command.Status;
import mudmk3.domain.game.Command.Stop;
import mudmk3.domain.game.Command.Talk;
import mudmk3.domain.game.Command.Techniques;
import mudmk3.domain.game.Command.Unknown;
import mudmk3.domain.game.Command.UseAbility;
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
    void talkWithoutSpaceIsAbility() {
        assertThat(parse("대화왕씨")).isEqualTo(new UseAbility("대화왕씨", null));
        assertThat(parse("말 왕씨")).isEqualTo(new UseAbility("말", new ItemQuery("왕씨", 0)));
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
    void notCommandIsAbility() {
        assertThat(parse("말 안녕")).isEqualTo(new UseAbility("말", new ItemQuery("안녕", 0)));
        assertThat(parse("보")).isEqualTo(new UseAbility("보", null));
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
    void status() {
        assertThat(parse("상태")).isEqualTo(new Status());
        assertThat(parse("ㅅ")).isEqualTo(new Status());
        assertThat(parse(" 상태 ")).isEqualTo(new Status());
    }

    @Test
    void statusWithArgumentFail() {
        assertThat(parse("상태 한립")).isEqualTo(new Unknown("상태"));
        assertThat(parse("ㅅ 한립")).isEqualTo(new Unknown("ㅅ"));
    }

    @Test
    void techniquesAndAbilities() {
        assertThat(parse("공법")).isEqualTo(new Techniques());
        assertThat(parse("신통")).isEqualTo(new Abilities());
        assertThat(parse("공법 장춘공")).isEqualTo(new Unknown("공법"));
        assertThat(parse("신통 화탄술")).isEqualTo(new Unknown("신통"));
    }

    @Test
    void read() {
        assertThat(parse("읽기")).isEqualTo(new Read(null));
        assertThat(parse("읽기 장춘공 옥간")).isEqualTo(new Read(new ItemQuery("장춘공 옥간", 0)));
        assertThat(parse("읽기 2.옥간")).isEqualTo(new Read(new ItemQuery("옥간", 2)));
    }

    @Test
    void attack() {
        assertThat(parse("공격 청모랑")).isEqualTo(new Command.Attack(new ItemQuery("청모랑", 0)));
        assertThat(parse("공격 2.청모랑")).isEqualTo(new Command.Attack(new ItemQuery("청모랑", 2)));
        assertThat(parse("공격  쌍두   흑망 ")).isEqualTo(new Command.Attack(new ItemQuery("쌍두 흑망", 0)));
        assertThat(parse("공격")).isEqualTo(new Command.Attack(null));
    }

    @Test
    void collect() {
        assertThat(parse("수습 청모랑")).isEqualTo(new Command.Collect(new ItemQuery("청모랑", 0)));
        assertThat(parse("수습 2.청모랑의 시체")).isEqualTo(new Command.Collect(new ItemQuery("청모랑의 시체", 2)));
        assertThat(parse("수습")).isEqualTo(new Command.Collect(null));
    }

    @Test
    void readTakesWholeArgumentAsName() {
        assertThat(parse("읽기 옥간 2")).isEqualTo(new Read(new ItemQuery("옥간 2", 0)));
        assertThat(parse("읽기 옥간 전부")).isEqualTo(new Read(new ItemQuery("옥간 전부", 0)));
        assertThat(parse("읽기 확인")).isEqualTo(new Read(new ItemQuery("확인", 0)));
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

    @Test
    void shopCommands() {
        assertThat(parse("목록")).isEqualTo(new ShopList(""));
        assertThat(parse("목록 왕씨")).isEqualTo(new ShopList("왕씨"));
        assertThat(parse("사기")).isEqualTo(new Buy(""));
        assertThat(parse("사기   왕씨  회춘단 5")).isEqualTo(new Buy("왕씨 회춘단 5"));
        assertThat(parse("팔기 늑대 가죽 전부")).isEqualTo(new Sell("늑대 가죽 전부"));
        assertThat(parse("값 늑대 가죽")).isEqualTo(new Appraise("늑대 가죽"));
    }

    @Test
    void give() {
        assertThat(parse("주기")).isEqualTo(new Give(null, ""));
        assertThat(parse("주기 한립")).isEqualTo(new Give("한립", ""));
        assertThat(parse("주기 한립 하품 100")).isEqualTo(new Give("한립", "하품 100"));
        assertThat(parse("주기  한립   늑대 가죽 2")).isEqualTo(new Give("한립", "늑대 가죽 2"));
    }

    @Test
    void removedBindIsAbility() {
        assertThat(parse("귀속 비검")).isEqualTo(new UseAbility("귀속", new ItemQuery("비검", 0)));
        assertThat(parse("귀속")).isEqualTo(new UseAbility("귀속", null));
        assertThat(parse("귀속 해제 비검")).isEqualTo(new UseAbility("귀속", new ItemQuery("해제 비검", 0)));
    }

    @Test
    void useAbility() {
        assertThat(parse("화탄술 청모랑")).isEqualTo(new UseAbility("화탄술", new ItemQuery("청모랑", 0)));
        assertThat(parse("화탄술 2.청모랑")).isEqualTo(new UseAbility("화탄술", new ItemQuery("청모랑", 2)));
        assertThat(parse("화탄술   쌍두  흑망 ")).isEqualTo(new UseAbility("화탄술", new ItemQuery("쌍두 흑망", 0)));
        assertThat(parse("화탄술")).isEqualTo(new UseAbility("화탄술", null));
        assertThat(parse("Bolt wolf")).isEqualTo(new UseAbility("Bolt", new ItemQuery("wolf", 0)));
    }

    @Test
    void commandWordIsNotAbility() {
        assertThat(parse("탐지")).isEqualTo(new Unknown("탐지"));
        assertThat(parse("탐지 청모랑")).isEqualTo(new Unknown("탐지"));
        assertThat(parse("상태 한립")).isEqualTo(new Unknown("상태"));
        assertThat(parse("북 가자")).isEqualTo(new Unknown("북"));
        assertThat(parse("N 가자")).isEqualTo(new Unknown("N"));
    }

    @Test
    void stop() {
        assertThat(parse("중지")).isEqualTo(new Stop());
        assertThat(parse(" 중지 ")).isEqualTo(new Stop());
        assertThat(parse("중지 지금")).isEqualTo(new Unknown("중지"));
    }

    @Test
    void confirmAndCancel() {
        assertThat(parse("확인")).isEqualTo(new Confirm());
        assertThat(parse("취소")).isEqualTo(new Cancel());
    }

    @Test
    void confirmWithArgumentFail() {
        assertThat(parse("확인 x")).isEqualTo(new Unknown("확인"));
        assertThat(parse("취소 x")).isEqualTo(new Unknown("취소"));
    }
}
