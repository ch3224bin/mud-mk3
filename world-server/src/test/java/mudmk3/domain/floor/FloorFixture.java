package mudmk3.domain.floor;

import mudmk3.application.floor.provided.FloorPlacementSaveRequest;
import mudmk3.domain.AbstractEntity;
import org.jspecify.annotations.Nullable;
import org.springframework.test.util.ReflectionTestUtils;

public class FloorFixture {

    public static FloorPlacementSaveInfo createFloorPlacementSaveInfo(int count, @Nullable Integer respawnMinutes) {
        return new FloorPlacementSaveInfo(count, respawnMinutes);
    }

    public static FloorPlacementSaveRequest createFloorPlacementSaveRequest(Long itemTypeId, int count,
                                                                            @Nullable Integer respawnMinutes) {
        return new FloorPlacementSaveRequest(itemTypeId, count, respawnMinutes);
    }

    /** 엔티티에 id를 넣는다(저장하지 않는 테스트에서 같은 종류·순서를 가리려고) */
    public static <T extends AbstractEntity> T withId(T entity, long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }
}
