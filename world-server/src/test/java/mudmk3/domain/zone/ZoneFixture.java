package mudmk3.domain.zone;

public class ZoneFixture {

    public static ZoneSaveInfo createZoneSaveInfo(String name) {
        return new ZoneSaveInfo(name, "푸른 돌이 흩어진 숲이다.");
    }

    public static Zone createZone() {
        return createZone("청석숲");
    }

    public static Zone createZone(String name) {
        return Zone.create(createZoneSaveInfo(name));
    }
}
