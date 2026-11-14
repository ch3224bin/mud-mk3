package mudmk3.domain.zone;

import mudmk3.domain.faction.Faction;
import mudmk3.domain.faction.FactionFixture;
import mudmk3.domain.faction.FactionKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneTest {

    @Test
    void create() {
        Zone zone = Zone.create(new ZoneSaveInfo("  청석숲 ", "푸른 돌이 흩어진 숲이다."), null);

        assertThat(zone.getName()).isEqualTo("청석숲");
        assertThat(zone.getDescription()).isEqualTo("푸른 돌이 흩어진 숲이다.");
        assertThat(zone.findOwner()).isEmpty();
    }

    @Test
    void createWithOwner() {
        Faction owner = FactionFixture.createFaction(FactionKind.CASTLE, "청석성");

        Zone zone = Zone.create(new ZoneSaveInfo("청석숲", null), owner);

        assertThat(zone.findOwner()).contains(owner);
    }

    @Test
    void createWithoutDescription() {
        Zone zone = Zone.create(new ZoneSaveInfo("청석숲", null), null);

        assertThat(zone.getDescription()).isEmpty();
    }

    @Test
    void update() {
        Zone zone = ZoneFixture.createZone();

        zone.update(new ZoneSaveInfo(" 황풍곡  ", null), null);

        assertThat(zone.getName()).isEqualTo("황풍곡");
        assertThat(zone.getDescription()).isEmpty();
    }

    @Test
    void updateRemovesOwner() {
        Zone zone = Zone.create(new ZoneSaveInfo("청석숲", null), FactionFixture.createFaction());

        zone.update(new ZoneSaveInfo("청석숲", null), null);

        assertThat(zone.findOwner()).isEmpty();
    }
}
