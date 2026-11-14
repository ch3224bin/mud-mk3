package mudmk3.application.inventory.provided;

import mudmk3.domain.inventory.GiveResult;
import mudmk3.domain.inventory.PurchaseResult;
import mudmk3.domain.inventory.RemoveCheck;
import mudmk3.domain.inventory.SaleResult;

/** 게임 명령으로 캐릭터 소지품을 바꾼다. 검사가 실패하면 아무것도 바꾸지 않는다 */
public interface InventoryEditor {

    /** 새 캐릭터의 소지품을 만들어 시작 소지품을 그 순서대로 넣고 저장한다. 이미 소지품이 있으면 {@code IllegalStateException} */
    void createWithStartingItems(Long characterId);

    /** 귀속 안 된 물건을 들어가는 만큼만(최대 {@code count}, 1 이상) 넣고 넣은 수를 돌려준다 */
    int addUpTo(Long characterId, Long itemTypeId, int count);

    /** 버리기. 귀속된 묶음은 뺄 수 없다({@code IllegalStateException}) */
    RemoveCheck remove(Long characterId, Long itemTypeId, boolean bound, int count);

    /** {@code totalPrice}(하품 기준)로 산다. 상품·극품을 깨야 하면 {@code confirmed} 일 때만 사고 아니면 확인 필요를 돌려준다 */
    PurchaseResult purchase(Long characterId, Long itemTypeId, int count, long totalPrice, boolean confirmed);

    /** 팔고 하품 영석 {@code payout} 개를 받는다. 귀속된 묶음은 팔 수 없다({@code IllegalStateException}) */
    SaleResult sell(Long characterId, Long itemTypeId, boolean bound, int count, long payout);

    /** 주는 쪽 그 묶음에서 받는 쪽에 귀속 안 된 물건으로 옮긴다. 귀속된 물건은 줄 수 없다({@code IllegalStateException}) */
    GiveResult give(Long fromCharacterId, Long toCharacterId, Long itemTypeId, boolean bound, int count);

    void bind(Long characterId, Long itemTypeId);

    void unbind(Long characterId, Long itemTypeId);
}
