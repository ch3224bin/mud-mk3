package mudmk3.application.inventory.provided;

import mudmk3.domain.inventory.Inventory;

/** 캐릭터 소지품을 찾는다 */
public interface InventoryFinder {

    /** 없으면 저장하지 않은 빈 소지품(기본 2칸) */
    Inventory find(Long characterId);
}
