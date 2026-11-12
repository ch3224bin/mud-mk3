package mudmk3.domain.zone;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneTest {

    @Test
    void create() {
        Zone zone = Zone.create(new ZoneSaveInfo("  청석숲 ", "푸른 돌이 흩어진 숲이다."));

        assertThat(zone.getName()).isEqualTo("청석숲");
        assertThat(zone.getDescription()).isEqualTo("푸른 돌이 흩어진 숲이다.");
    }

    @Test
    void createWithoutDescription() {
        Zone zone = Zone.create(new ZoneSaveInfo("청석숲", null));

        assertThat(zone.getDescription()).isEmpty();
    }

    @Test
    void update() {
        Zone zone = ZoneFixture.createZone();

        zone.update(new ZoneSaveInfo(" 황풍곡  ", null));

        assertThat(zone.getName()).isEqualTo("황풍곡");
        assertThat(zone.getDescription()).isEmpty();
    }
}
