import type { BagGradeCode, ItemCategoryCode, ShopKindCode } from "./api";

/**
 * 아이템 종류 표(world-server ItemCategory 와 같음, 룰북 item.md 종류 순서).
 * stackSize 는 한 칸에 겹치는 수(저물대는 칸을 차지하지 않아 0), bindable 은 직접 귀속 가능,
 * creatable 은 관리 툴로 만들 수 있는지(영석은 엔진이 심는다)
 */
export const ITEM_CATEGORIES: {
  code: ItemCategoryCode;
  name: string;
  stackSize: number;
  bindable: boolean;
  creatable: boolean;
}[] = [
  { code: "ARTIFACT", name: "법기", stackSize: 1, bindable: true, creatable: true },
  { code: "PILL", name: "단약", stackSize: 99, bindable: false, creatable: true },
  { code: "ELIXIR", name: "영약", stackSize: 99, bindable: false, creatable: true },
  { code: "TALISMAN", name: "부적", stackSize: 99, bindable: false, creatable: true },
  { code: "FORMATION_DISK", name: "진법원반", stackSize: 1, bindable: true, creatable: true },
  { code: "SPIRIT_STONE", name: "영석", stackSize: 10000, bindable: false, creatable: false },
  { code: "MATERIAL", name: "재료", stackSize: 99, bindable: false, creatable: true },
  { code: "SCROLL", name: "옥간·서책", stackSize: 99, bindable: false, creatable: true },
  { code: "TOOL", name: "제작 도구", stackSize: 1, bindable: true, creatable: true },
  { code: "BAG", name: "저물대", stackSize: 0, bindable: true, creatable: true },
  { code: "BEAST_EGG", name: "영수 알·새끼", stackSize: 1, bindable: false, creatable: true },
  { code: "BEAST_FOOD", name: "영수 먹이", stackSize: 99, bindable: false, creatable: true },
  { code: "PUPPET", name: "괴뢰", stackSize: 1, bindable: false, creatable: true },
  { code: "PASS", name: "출입권", stackSize: 99, bindable: false, creatable: true },
  { code: "QUEST", name: "퀘스트 물건", stackSize: 1, bindable: false, creatable: true },
];

/** 저물대 등급과 칸 수(룰북 item.md 저물대 표) */
export const BAG_GRADES: { code: BagGradeCode; name: string; slots: number }[] = [
  { code: "LOW", name: "하급", slots: 20 },
  { code: "MIDDLE", name: "중급", slots: 40 },
  { code: "HIGH", name: "상급", slots: 80 },
];

/** 상점 종류와 파는 물건 종류(world-server ShopKind 와 같음, 룰북 game-structure.md 상점 표 순서) */
export const SHOP_KINDS: { code: ShopKindCode; name: string; sells: ItemCategoryCode[] }[] = [
  { code: "PILL_SHOP", name: "단약방", sells: ["PILL"] },
  { code: "ARTIFACT_SHOP", name: "법기점", sells: ["ARTIFACT", "PUPPET", "TOOL"] },
  { code: "BOOK_SHOP", name: "서방", sells: ["SCROLL"] },
  { code: "TALISMAN_SHOP", name: "부적방", sells: ["TALISMAN"] },
  { code: "MATERIAL_SHOP", name: "재료상", sells: ["MATERIAL"] },
  { code: "GENERAL_STORE", name: "잡화점", sells: ["BAG", "BEAST_FOOD", "PASS"] },
  { code: "BEAST_SHOP", name: "영수점", sells: ["BEAST_EGG", "PILL"] },
];

/** 저물대 등급 선택에 쓰는 이름. 예: 하급 (20칸) */
export function bagGradeLabel(code: BagGradeCode): string {
  const grade = BAG_GRADES.find((g) => g.code === code);
  return grade ? `${grade.name} (${grade.slots}칸)` : code;
}

/** 1,000 이상이면 쉼표를 찍은 수. 예: 9,880 */
export function formatCount(value: number): string {
  return value.toLocaleString("en-US");
}
