package mudmk3.domain.faction;

import mudmk3.application.faction.provided.FactionSaveRequest;

public class FactionFixture {

    public static FactionSaveRequest createFactionSaveRequest(FactionKind kind, String name) {
        return new FactionSaveRequest(kind, name, name + "의 묘사다.");
    }

    public static FactionSaveInfo createFactionSaveInfo(FactionKind kind, String name) {
        return new FactionSaveInfo(kind, name, name + "의 묘사다.");
    }

    public static Faction createFaction() {
        return createFaction(FactionKind.CASTLE, "청석성");
    }

    public static Faction createFaction(FactionKind kind, String name) {
        return Faction.create(createFactionSaveInfo(kind, name));
    }
}
