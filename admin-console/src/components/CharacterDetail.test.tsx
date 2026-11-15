import { getDefaultNormalizer, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { CharacterDetailResponse, RealmResponse } from "../lib/api";
import CharacterDetail from "./CharacterDetail";

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  designateRealm: vi.fn(),
}));

const realms: RealmResponse[] = [
  { code: "QI_1", name: "연기 1층" },
  { code: "QI_7", name: "연기 7층" },
];

function insight(wood: number, fire: number): CharacterDetailResponse["insight"] {
  return [
    { kind: "METAL", name: "금", amount: 0 },
    { kind: "WOOD", name: "목", amount: wood },
    { kind: "WATER", name: "수", amount: 0 },
    { kind: "FIRE", name: "화", amount: fire },
    { kind: "EARTH", name: "토", amount: 0 },
    { kind: "BODY", name: "연체", amount: 0 },
  ];
}

const garam: CharacterDetailResponse = {
  id: 3,
  name: "가람",
  realm: "QI_1",
  realmName: "연기 1층",
  spiritRoot: null,
  stats: { qiBlood: 60, mana: 20, sense: 15, essence: 10, spirit: 20, body: 12, strength: 11 },
  techniques: [],
  insight: insight(112, 230),
};

/** 두 칸 띄움을 그대로 비교한다 */
const exact = { normalizer: getDefaultNormalizer({ collapseWhitespace: false }) };

beforeEach(() => {
  vi.clearAllMocks();
});

describe("CharacterDetail", () => {
  it("스탯 표 아래에 깨달음 여섯 갈래를 보인다", () => {
    render(<CharacterDetail initial={garam} realms={realms} />);

    const line = screen.getByText("깨달음  금 0 · 목 112 · 수 0 · 화 230 · 토 0 · 연체 0", exact);
    const statTable = screen.getByRole("table", { name: "스탯" });
    expect(statTable.compareDocumentPosition(line) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
  });

  it("1,000 이상의 깨달음은 쉼표를 찍는다", () => {
    render(<CharacterDetail initial={{ ...garam, insight: insight(9880, 0) }} realms={realms} />);

    expect(screen.getByText("깨달음  금 0 · 목 9,880 · 수 0 · 화 0 · 토 0 · 연체 0", exact)).toBeInTheDocument();
  });

  it("경지를 지정하면 받은 상세의 깨달음으로 다시 그린다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.designateRealm).mockResolvedValue({ ...garam, realm: "QI_7", realmName: "연기 7층", insight: insight(5, 6) });
    render(<CharacterDetail initial={garam} realms={realms} />);

    await user.selectOptions(screen.getByLabelText("경지"), "QI_7");
    await user.click(screen.getByRole("button", { name: "경지 지정" }));

    expect(await screen.findByText("깨달음  금 0 · 목 5 · 수 0 · 화 6 · 토 0 · 연체 0", exact)).toBeInTheDocument();
    expect(within(screen.getByRole("table", { name: "스탯" })).getAllByRole("row")).toHaveLength(7);
  });
});
