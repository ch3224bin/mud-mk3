package mudmk3.application.item.required;

import org.jspecify.annotations.Nullable;

/** 어떤 물건을 파는 상점 하나의 NPC 이름과 판매가(null 이면 기준가를 따라감). 기준가 변경 검사에 쓴다 */
public record ShopPrice(String npcName, @Nullable Integer price) {

    /** 그 기준가일 때 실제로 파는 값 */
    public long effectivePrice(long baseValue) {
        return price != null ? price : baseValue;
    }
}
