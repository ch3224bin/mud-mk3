package mudmk3.domain.faction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FactionTest {

    @Test
    void create() {
        Faction faction = Faction.create(new FactionSaveInfo(FactionKind.CASTLE, "  청석성 ", "돌로 쌓은 성이다."));

        assertThat(faction.getKind()).isEqualTo(FactionKind.CASTLE);
        assertThat(faction.getName()).isEqualTo("청석성");
        assertThat(faction.getDescription()).isEqualTo("돌로 쌓은 성이다.");
    }

    @Test
    void createWithoutDescription() {
        Faction faction = Faction.create(new FactionSaveInfo(FactionKind.SECT, "황엽곡", null));

        assertThat(faction.getDescription()).isEmpty();
    }

    @Test
    void update() {
        Faction faction = FactionFixture.createFaction(FactionKind.CASTLE, "청석성");

        faction.update(new FactionSaveInfo(FactionKind.SECT, " 황엽곡  ", null));

        assertThat(faction.getKind()).isEqualTo(FactionKind.SECT);
        assertThat(faction.getName()).isEqualTo("황엽곡");
        assertThat(faction.getDescription()).isEmpty();
    }

    @Test
    void kindKoreanName() {
        assertThat(FactionKind.CASTLE.koreanName()).isEqualTo("성");
        assertThat(FactionKind.SECT.koreanName()).isEqualTo("종문");
    }
}
