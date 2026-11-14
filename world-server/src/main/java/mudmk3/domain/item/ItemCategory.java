package mudmk3.domain.item;

/**
 * 아이템 종류(카테고리). 선언 순서가 목록 순서다(룰북 item.md 종류 순서).
 * 3차에 종류가 가르는 것은 겹침·직접 귀속 가능·매입 비율·칸 차지(저물대)뿐이다
 */
public enum ItemCategory {
    ARTIFACT("법기", 1, true, 25, true),
    PILL("단약", 99, false, 25, true),
    ELIXIR("영약", 99, false, 25, true),
    TALISMAN("부적", 99, false, 25, true),
    FORMATION_DISK("진법원반", 1, true, 25, true),
    SPIRIT_STONE("영석", 10_000, false, 0, false),
    MATERIAL("재료", 99, false, 40, true),
    SCROLL("옥간·서책", 99, false, 25, true),
    TOOL("제작 도구", 1, true, 25, true),
    BAG("저물대", 0, true, 25, true),
    BEAST_EGG("영수 알·새끼", 1, false, 0, true),
    BEAST_FOOD("영수 먹이", 99, false, 25, true),
    PUPPET("괴뢰", 1, false, 25, true),
    PASS("출입권", 99, false, 25, true),
    QUEST("퀘스트 물건", 1, false, 0, true);

    private final String koreanName;
    private final int stackSize;
    private final boolean bindable;
    private final int buyRatePercent;
    private final boolean creatableByAdmin;

    ItemCategory(String koreanName, int stackSize, boolean bindable, int buyRatePercent, boolean creatableByAdmin) {
        this.koreanName = koreanName;
        this.stackSize = stackSize;
        this.bindable = bindable;
        this.buyRatePercent = buyRatePercent;
        this.creatableByAdmin = creatableByAdmin;
    }

    public String koreanName() {
        return koreanName;
    }

    /** 한 칸에 겹치는 수. 저물대는 칸을 차지하지 않아 0 */
    public int stackSize() {
        return stackSize;
    }

    /** {@code 귀속} 명령으로 직접 귀속할 수 있는 종류인지 */
    public boolean bindable() {
        return bindable;
    }

    /** 상점 매입 비율(%). 상점이 사지 않는 종류는 0 */
    public int buyRatePercent() {
        return buyRatePercent;
    }

    /** 관리 툴로 만들 수 있는 종류인지. 영석은 엔진이 심는다 */
    public boolean creatableByAdmin() {
        return creatableByAdmin;
    }
}
