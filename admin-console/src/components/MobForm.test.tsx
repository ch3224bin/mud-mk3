import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { ItemTypeResponse, MobDetailResponse, RealmResponse } from "../lib/api";
import MobForm from "./MobForm";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace: vi.fn() }),
}));

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  createMob: vi.fn(),
  updateMob: vi.fn(),
  deleteMob: vi.fn(),
}));

const realms: RealmResponse[] = [
  { code: "MORTAL", name: "범인" },
  { code: "QI_1", name: "연기 1층" },
  { code: "QI_2", name: "연기 2층" },
  { code: "FOUNDATION_PEAK", name: "축기 대원만" },
];

function itemType(id: number, name: string): ItemTypeResponse {
  return {
    id,
    name,
    description: "",
    category: "MATERIAL",
    categoryName: "재료",
    stackSize: 99,
    baseValue: 1,
    bagGrade: null,
    bagGradeName: null,
    bagSlots: 0,
    spiritStone: false,
    buyRatePercent: 50,
    techniqueId: null,
    techniqueName: null,
  };
}

const itemTypes = [itemType(7, "늑대 가죽"), itemType(8, "늑대 힘줄")];

const wolf: MobDetailResponse = {
  id: 2,
  name: "청모랑",
  description: "푸른 털의 늑대.",
  kind: "BEAST",
  kindName: "요수",
  realm: "QI_2",
  realmName: "연기 2층",
  tier: "NORMAL",
  tierName: "일반",
  rankName: "1급",
  attackName: "물어뜯기",
  attackCoefficient: null,
  respawnSeconds: null,
  effectiveRespawnSeconds: 90,
  stats: { qiBlood: 49, body: 8, power: 8 },
  fixedDrops: [{ itemTypeId: 7, itemTypeName: "늑대 가죽", count: 2 }],
  tierDrops: [{ tier: "ADVANCED", tierName: "고급", itemTypeId: 8, itemTypeName: "늑대 힘줄" }],
  placements: [
    { id: 11, roomId: 3, roomName: "마을 어귀", count: 2 },
    { id: 12, roomId: 4, roomName: "뒷산 숲길", count: 1 },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

afterEach(() => {
  vi.restoreAllMocks();
});

/** 몹 삭제 버튼(드롭 표 행의 삭제가 아닌 것) */
function deleteButton(): HTMLElement {
  return screen.getAllByRole("button", { name: "삭제" }).find((b) => b.closest(".actions") !== null)!;
}

describe("MobForm", () => {
  it("만들기: 기본값으로 보내고(빈 재등장은 null, 두목이 아니면 공격 계수 null) 성공하면 새 몹 화면으로 간다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createMob).mockResolvedValue({ ...wolf, id: 5 });
    render(<MobForm realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    expect(
      screen.getByText("비우면 체급 기준(일반 90초 · 강적 3분 · 정예 10분). 두목은 30~60분을 꼭 넣습니다."),
    ).toBeInTheDocument();
    expect(screen.getByText("영석은 고정 재료 값으로 엔진이 정합니다(30%).")).toBeInTheDocument();
    await user.type(screen.getByLabelText("이름"), "청모랑");
    await user.type(screen.getByLabelText("묘사"), "푸른 털의 늑대.");
    await user.selectOptions(screen.getByLabelText("기준 경지"), "QI_2");
    await user.type(screen.getByLabelText("공격 이름"), "물어뜯기");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(api.createMob).toHaveBeenCalledWith({
      name: "청모랑",
      description: "푸른 털의 늑대.",
      kind: "BEAST",
      realm: "QI_2",
      tier: "NORMAL",
      attackName: "물어뜯기",
      attackCoefficient: null,
      respawnSeconds: null,
      fixedDrops: [],
      tierDrops: [],
    });
    expect(push).toHaveBeenCalledWith("/mob/?id=5");
  });

  it("기준 경지 선택에 범인이 없고 첫 값은 연기 1층이다", () => {
    render(<MobForm realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    const select = screen.getByLabelText("기준 경지") as HTMLSelectElement;
    expect(Array.from(select.options).map((o) => o.textContent)).toEqual(["연기 1층", "연기 2층", "축기 대원만"]);
    expect(select).toHaveValue("QI_1");
    expect(Array.from((screen.getByLabelText("종류") as HTMLSelectElement).options).map((o) => o.textContent)).toEqual(
      ["요수", "적대 수선자", "귀물·음혼", "괴뢰"],
    );
    expect(Array.from((screen.getByLabelText("체급") as HTMLSelectElement).options).map((o) => o.textContent)).toEqual(
      ["일반", "강적", "정예", "두목"],
    );
  });

  it("공격 계수는 체급이 두목일 때만 보이고 기본 0.6 이다. 재등장을 넣으면 수로 보낸다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createMob).mockResolvedValue({ ...wolf, id: 6 });
    render(<MobForm realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    expect(screen.queryByLabelText("공격 계수")).not.toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText("체급"), "BOSS");
    expect(screen.getByLabelText("공격 계수")).toHaveValue(0.6);
    await user.type(screen.getByLabelText("재등장(초)"), "2700");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(api.createMob).toHaveBeenCalledWith(
      expect.objectContaining({ tier: "BOSS", attackCoefficient: 0.6, respawnSeconds: 2700 }),
    );

    await user.selectOptions(screen.getByLabelText("체급"), "ELITE");
    expect(screen.queryByLabelText("공격 계수")).not.toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "저장" }));
    expect(api.createMob).toHaveBeenLastCalledWith(expect.objectContaining({ tier: "ELITE", attackCoefficient: null }));
  });

  it("저장에 실패하면 서버 400 detail 을 보인다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createMob).mockRejectedValue(new api.ApiError(400, "몹 이름은 1~20자로 정하십시오."));
    render(<MobForm realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("몹 이름은 1~20자로 정하십시오.");
    expect(push).not.toHaveBeenCalled();
  });

  it("브라우저 검사로 막지 않고 소수 둘째 자리 계수·범위 밖 재등장·고정 재료 수 0 을 그대로 보내 서버 detail 을 보인다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createMob).mockRejectedValue(new api.ApiError(400, "공격 계수는 소수 첫째 자리까지 적으십시오."));
    render(<MobForm realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    await user.selectOptions(screen.getByLabelText("체급"), "BOSS");
    await user.clear(screen.getByLabelText("공격 계수"));
    await user.type(screen.getByLabelText("공격 계수"), "0.65");
    await user.type(screen.getByLabelText("재등장(초)"), "30.5");
    await user.click(screen.getByRole("button", { name: "고정 재료 추가" }));
    await user.clear(screen.getByLabelText("고정 재료 1 수"));
    await user.type(screen.getByLabelText("고정 재료 1 수"), "0");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(api.createMob).toHaveBeenCalledWith(
      expect.objectContaining({ attackCoefficient: 0.65, respawnSeconds: 30.5, fixedDrops: [{ itemTypeId: 7, count: 0 }] }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent("공격 계수는 소수 첫째 자리까지 적으십시오.");
  });

  it("재등장 0 도 브라우저 검사 없이 보낸다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createMob).mockRejectedValue(new api.ApiError(400, "재등장은 10~86,400초로 정하십시오."));
    render(<MobForm realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    await user.type(screen.getByLabelText("재등장(초)"), "0");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(api.createMob).toHaveBeenCalledWith(expect.objectContaining({ respawnSeconds: 0 }));
    expect(await screen.findByRole("alert")).toHaveTextContent("재등장은 10~86,400초로 정하십시오.");
  });

  it("고칠 때는 저장된 값으로 열리고 스탯 줄과 배치된 방 링크를 보이며, 저장하면 PUT 결과를 onSaved 로 넘긴다", async () => {
    const user = userEvent.setup();
    const onSaved = vi.fn();
    vi.mocked(api.updateMob).mockResolvedValue(wolf);
    render(<MobForm initial={wolf} realms={realms} itemTypes={itemTypes} onSaved={onSaved} />);

    expect(screen.getByLabelText("이름")).toHaveValue("청모랑");
    expect(screen.getByLabelText("기준 경지")).toHaveValue("QI_2");
    expect(screen.getByText("기혈 49 · 육신 8 · 위력 8")).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "배치된 방" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "마을 어귀" })).toHaveAttribute("href", expect.stringMatching(/^\/room\/?\?id=3$/));
    expect(screen.getByRole("link", { name: "뒷산 숲길" })).toHaveAttribute("href", expect.stringMatching(/^\/room\/?\?id=4$/));
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(api.updateMob).toHaveBeenCalledWith(2, {
      name: "청모랑",
      description: "푸른 털의 늑대.",
      kind: "BEAST",
      realm: "QI_2",
      tier: "NORMAL",
      attackName: "물어뜯기",
      attackCoefficient: null,
      respawnSeconds: null,
      fixedDrops: [{ itemTypeId: 7, count: 2 }],
      tierDrops: [{ tier: "ADVANCED", itemTypeId: 8 }],
    });
    expect(onSaved).toHaveBeenCalledWith(wolf);
    expect(push).not.toHaveBeenCalled();
  });

  it("두목을 고칠 때는 저장된 공격 계수·재등장으로 열린다. 배치가 없으면 없음 을 보인다", () => {
    const boss: MobDetailResponse = {
      ...wolf,
      tier: "BOSS",
      tierName: "두목",
      attackCoefficient: 0.5,
      respawnSeconds: 2700,
      placements: [],
    };
    render(<MobForm initial={boss} realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    expect(screen.getByLabelText("공격 계수")).toHaveValue(0.5);
    expect(screen.getByLabelText("재등장(초)")).toHaveValue(2700);
    expect(screen.getByText("없음", { selector: "p" })).toBeInTheDocument();
  });

  it("새로 만들 때는 삭제·스탯·배치된 방이 없다", () => {
    render(<MobForm realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    expect(screen.queryAllByRole("button", { name: "삭제" })).toHaveLength(0);
    expect(screen.queryByText("배치된 방")).not.toBeInTheDocument();
  });

  it("삭제 는 확인을 묻고, 지우면 몹 목록으로 간다", async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(api.deleteMob).mockResolvedValue(undefined);
    render(<MobForm initial={wolf} realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    await user.click(deleteButton());

    expect(confirm).toHaveBeenCalledWith(
      "'청모랑' 몹을 지울까요? 이 몹의 배치 2곳도 함께 지워지고, 세계에 나와 있는 청모랑은 사라집니다.",
    );
    expect(api.deleteMob).toHaveBeenCalledWith(2);
    expect(push).toHaveBeenCalledWith("/mobs/");
  });

  it("배치가 없으면 5차 문구로 묻는다", async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(false);
    render(<MobForm initial={{ ...wolf, placements: [] }} realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    await user.click(deleteButton());

    expect(confirm).toHaveBeenCalledWith("'청모랑' 몹을 지울까요?");
  });

  it("삭제 확인을 취소하면 지우지 않고, 서버 오류면 detail 을 보인다", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValueOnce(false).mockReturnValueOnce(true);
    vi.mocked(api.deleteMob).mockRejectedValue(new api.ApiError(400, "몹을 찾을 수 없습니다. id: 2"));
    render(<MobForm initial={wolf} realms={realms} itemTypes={itemTypes} onSaved={vi.fn()} />);

    await user.click(deleteButton());
    expect(api.deleteMob).not.toHaveBeenCalled();

    await user.click(deleteButton());
    expect(await screen.findByRole("alert")).toHaveTextContent("몹을 찾을 수 없습니다. id: 2");
    expect(push).not.toHaveBeenCalled();
  });
});
