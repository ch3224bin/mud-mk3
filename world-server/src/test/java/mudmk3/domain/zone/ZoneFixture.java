package mudmk3.domain.zone;

import mudmk3.application.zone.provided.ZoneSaveRequest;

public class ZoneFixture {

    public static ZoneSaveRequest createZoneSaveRequest(String name) {
        return new ZoneSaveRequest(name, "푸른 돌이 흩어진 숲이다.", null);
    }

    public static ZoneSaveInfo createZoneSaveInfo(String name) {
        return new ZoneSaveInfo(name, "푸른 돌이 흩어진 숲이다.");
    }

    public static Zone createZone() {
        return createZone("청석숲");
    }

    public static Zone createZone(String name) {
        return Zone.create(createZoneSaveInfo(name), null);
    }
}
