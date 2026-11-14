package mudmk3.application.world.provided;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mudmk3.domain.world.StartingItem;
import mudmk3.domain.world.WorldSettings;

import java.util.List;

/** 세계 설정(시작 방·시작 소지품)을 바꾸는 기능을 제공한다 */
public interface WorldSettingsEditor {

    /** 시작 방을 정한다. 세계 설정 행이 없으면 만든다 */
    WorldSettings designateStartRoom(Long roomId);

    /**
     * 시작 소지품을 통째로 바꾸고 바꾼 목록(입력 순서)을 돌려준다. 세계 설정 행이 없으면 만든다.
     * 같은 물건 두 번·저물대 네 개 이상·칸 넘침·없는 물건이면 {@code IllegalArgumentException}
     */
    List<StartingItem> replaceStartingItems(@NotNull @Size(max = 30) List<@NotNull @Valid StartingItemRequest> items);
}
