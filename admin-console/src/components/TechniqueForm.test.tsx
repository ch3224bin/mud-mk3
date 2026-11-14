import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { RealmResponse, TechniqueDetailResponse } from "../lib/api";
import TechniqueForm from "./TechniqueForm";

const push = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push, replace: vi.fn() }),
}));

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  createTechnique: vi.fn(),
  updateTechnique: vi.fn(),
  deleteTechnique: vi.fn(),
}));

const realms: RealmResponse[] = [
  { code: "MORTAL", name: "범인" },
  { code: "QI_1", name: "연기 1층" },
  { code: "FOUNDATION_PEAK", name: "축기 대원만" },
];

function stage(insightRequired: number, manaBonus: number) {
  return {
    insightRequired,
    aptitudeRequired: 0,
    manaBonus,
    senseBonus: 0,
    spiritBonus: 0,
    essenceBonus: 0,
    bodyBonus: 0,
    strengthBonus: 0,
    aptitudeGain: 0,
  };
}

const changchun: TechniqueDetailResponse = {
  id: 2,
  name: "장춘공",
  description: "봄기운 같은 영기를 기르는 입문 공법.",
  elements: ["WOOD"],
  elementNames: "목",
  lineName: "오행",
  startRealm: "MORTAL",
  startRealmName: "범인",
  stages: [stage(0, 0), stage(120, 2), stage(190, 2)],
  abilities: [
    {
      name: "화탄술",
      learnStage: 1,
      kind: "ATTACK",
      kindName: "공격",
      elements: ["FIRE"],
      bodyRefining: false,
      target: "SINGLE",
      targetName: "한 대상",
      costStat: "MANA",
      costAmount: 5,
      costPerTick: false,
      castSeconds: 0,
      cooldownSeconds: 3,
      usableWhen: "COMBAT_ONLY",
    },
  ],
  learnerCount: 2,
  scrollItems: [
    { id: 11, name: "장춘공 서책" },
    { id: 10, name: "장춘공 옥간" },
  ],
};

beforeEach(() => {
  vi.clearAllMocks();
});

/** 공법의 속성 체크박스 묶음(신통 행의 속성 묶음이 아닌 것) */
function techniqueElements() {
  return within(screen.getAllByRole("group", { name: "속성" }).find((g) => g.tagName === "FIELDSET")!);
}

/** 공법 삭제 버튼(신통 행의 삭제가 아닌 것) */
function deleteButton(): HTMLElement {
  return screen.getAllByRole("button", { name: "삭제" }).find((b) => b.closest("table") === null)!;
}

afterEach(() => {
  vi.restoreAllMocks();
});

describe("TechniqueForm", () => {
  it("만들기 요청 본문은 속성을 오행 순서로, 1성 필요 깨달음 0 으로 보내고 성공하면 새 공법 화면으로 간다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createTechnique).mockResolvedValue({ ...changchun, id: 5 });
    render(<TechniqueForm realms={realms} onSaved={vi.fn()} />);

    expect(screen.getByText("속성을 고르지 않으면 연체술 공법입니다.")).toBeInTheDocument();
    expect(screen.getByLabelText("단계 수")).toHaveValue("9");
    await user.type(screen.getByLabelText("이름"), "화토결");
    const elements = techniqueElements();
    await user.click(elements.getByLabelText("토"));
    await user.click(elements.getByLabelText("화"));
    await user.selectOptions(screen.getByLabelText("시작 경지"), "QI_1");
    await user.selectOptions(screen.getByLabelText("단계 수"), "2");
    await user.clear(screen.getByLabelText("2성 필요 깨달음"));
    await user.type(screen.getByLabelText("2성 필요 깨달음"), "120");
    await user.click(screen.getByRole("button", { name: "신통 추가" }));
    await user.type(screen.getByLabelText("신통 이름"), "화탄술");
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(api.createTechnique).toHaveBeenCalledWith({
      name: "화토결",
      description: "",
      elements: ["FIRE", "EARTH"],
      startRealm: "QI_1",
      stages: [stage(0, 0), stage(120, 0)],
      abilities: [
        {
          name: "화탄술",
          learnStage: 1,
          kind: "ATTACK",
          elements: [],
          bodyRefining: false,
          target: "SINGLE",
          costStat: "NONE",
          costAmount: 0,
          costPerTick: false,
          castSeconds: 0,
          cooldownSeconds: 3,
          usableWhen: "COMBAT_ONLY",
        },
      ],
    });
    expect(push).toHaveBeenCalledWith("/technique/?id=5");
  });

  it("시작 경지 선택지는 받은 경지 목록이고 새 공법은 첫 경지(범인)로 열린다", () => {
    render(<TechniqueForm realms={realms} onSaved={vi.fn()} />);

    const select = screen.getByLabelText("시작 경지") as HTMLSelectElement;
    expect(Array.from(select.options).map((o) => o.textContent)).toEqual(["범인", "연기 1층", "축기 대원만"]);
    expect(select).toHaveValue("MORTAL");
    expect(screen.queryAllByRole("button", { name: "삭제" })).toHaveLength(0);
    expect(screen.queryByText("이 공법이 담긴 물건")).not.toBeInTheDocument();
  });

  it("저장에 실패하면 서버 detail 을 보인다", async () => {
    const user = userEvent.setup();
    vi.mocked(api.createTechnique).mockRejectedValue(
      new api.ApiError(400, "1성에 배우는 신통이 하나 이상 있어야 합니다."),
    );
    render(<TechniqueForm realms={realms} onSaved={vi.fn()} />);

    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("1성에 배우는 신통이 하나 이상 있어야 합니다.");
    expect(push).not.toHaveBeenCalled();
  });

  it("고칠 때는 저장된 값으로 열리고, 저장하면 PUT 결과를 onSaved 로 넘긴다", async () => {
    const user = userEvent.setup();
    const saved = { ...changchun, description: "" };
    vi.mocked(api.updateTechnique).mockResolvedValue(saved);
    const onSaved = vi.fn();
    render(<TechniqueForm initial={changchun} realms={realms} onSaved={onSaved} />);

    expect(screen.getByLabelText("이름", { selector: "form > label > input" })).toHaveValue("장춘공");
    expect(techniqueElements().getByLabelText("목")).toBeChecked();
    expect(screen.getByLabelText("단계 수")).toHaveValue("3");
    expect(screen.getByLabelText("3성 필요 깨달음")).toHaveValue(190);
    expect(screen.getByLabelText("소모 양")).toHaveValue(5);
    await user.click(screen.getByRole("button", { name: "저장" }));

    expect(api.updateTechnique).toHaveBeenCalledWith(
      2,
      expect.objectContaining({
        name: "장춘공",
        elements: ["WOOD"],
        stages: [stage(0, 0), stage(120, 2), stage(190, 2)],
        abilities: [expect.objectContaining({ name: "화탄술", elements: ["FIRE"], costStat: "MANA", costAmount: 5 })],
      }),
    );
    expect(onSaved).toHaveBeenCalledWith(saved);
    expect(push).not.toHaveBeenCalled();
  });

  it("고칠 때는 이 공법이 담긴 물건 링크와 익힌 캐릭터 수를 보인다", () => {
    render(<TechniqueForm initial={changchun} realms={realms} onSaved={vi.fn()} />);

    expect(screen.getByText("이 공법이 담긴 물건")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "장춘공 옥간" })).toHaveAttribute("href", expect.stringMatching(/^\/item\/?\?id=10$/));
    expect(screen.getByRole("link", { name: "장춘공 서책" })).toHaveAttribute("href", expect.stringMatching(/^\/item\/?\?id=11$/));
    expect(screen.getByText("익힌 캐릭터 2명")).toBeInTheDocument();
  });

  it("담긴 물건이 없으면 없음 을 보인다", () => {
    render(
      <TechniqueForm initial={{ ...changchun, scrollItems: [], learnerCount: 0 }} realms={realms} onSaved={vi.fn()} />,
    );

    expect(screen.getByText("없음", { selector: "p" })).toBeInTheDocument();
    expect(screen.getByText("익힌 캐릭터 0명")).toBeInTheDocument();
  });

  it("삭제 는 확인을 묻고, 지우면 공법 목록으로 간다", async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(api.deleteTechnique).mockResolvedValue(undefined);
    render(<TechniqueForm initial={changchun} realms={realms} onSaved={vi.fn()} />);

    await user.click(deleteButton());

    expect(confirm).toHaveBeenCalledWith("'장춘공' 공법을 지울까요?");
    expect(api.deleteTechnique).toHaveBeenCalledWith(2);
    expect(push).toHaveBeenCalledWith("/techniques/");
  });

  it("삭제 요청이 끝나기 전에는 삭제·저장 버튼을 누를 수 없다", async () => {
    const user = userEvent.setup();
    vi.spyOn(window, "confirm").mockReturnValue(true);
    let finish: () => void = () => {};
    vi.mocked(api.deleteTechnique).mockReturnValue(new Promise<void>((resolve) => (finish = resolve)));
    render(<TechniqueForm initial={changchun} realms={realms} onSaved={vi.fn()} />);

    await user.click(deleteButton());

    expect(deleteButton()).toBeDisabled();
    expect(screen.getByRole("button", { name: "저장" })).toBeDisabled();
    finish();
    await waitFor(() => expect(screen.getByRole("button", { name: "저장" })).toBeEnabled());
    expect(api.deleteTechnique).toHaveBeenCalledTimes(1);
  });

  it("삭제 확인을 취소하면 지우지 않고, 서버가 409 를 주면 detail 을 보인다", async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, "confirm").mockReturnValueOnce(false).mockReturnValueOnce(true);
    vi.mocked(api.deleteTechnique).mockRejectedValue(
      new api.ApiError(409, "익힌 캐릭터가 있는 공법은 지울 수 없습니다: 장춘공"),
    );
    render(<TechniqueForm initial={changchun} realms={realms} onSaved={vi.fn()} />);

    await user.click(deleteButton());
    expect(api.deleteTechnique).not.toHaveBeenCalled();

    await user.click(deleteButton());
    expect(confirm).toHaveBeenCalledTimes(2);
    expect(await screen.findByRole("alert")).toHaveTextContent("익힌 캐릭터가 있는 공법은 지울 수 없습니다: 장춘공");
    expect(push).not.toHaveBeenCalled();
  });
});
