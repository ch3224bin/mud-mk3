import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import * as api from "../lib/api";
import type { CharacterDetailResponse, RealmResponse } from "../lib/api";
import CharacterDetail from "./CharacterDetail";
import CharacterRealmForm from "./CharacterRealmForm";
import CharacterTable from "./CharacterTable";

vi.mock("../lib/api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("../lib/api")>()),
  designateRealm: vi.fn(),
}));

const realms: RealmResponse[] = [
  { code: "MORTAL", name: "범인" },
  { code: "QI_1", name: "연기 1층" },
  { code: "QI_7", name: "연기 7층" },
];

const garam: CharacterDetailResponse = {
  id: 3,
  name: "가람",
  realm: "QI_1",
  realmName: "연기 1층",
  spiritRoot: {
    name: "목·화 진영근",
    roots: [
      { element: "WOOD", elementName: "목", aptitude: 50 },
      { element: "FIRE", elementName: "화", aptitude: 52.25 },
    ],
  },
  stats: { qiBlood: 60, mana: 20, sense: 15, essence: 10, spirit: 20, body: 12, strength: 11 },
  techniques: [{ id: 2, name: "장춘공", stage: 1, stageCount: 9 }],
};

const qi7: CharacterDetailResponse = {
  ...garam,
  realm: "QI_7",
  realmName: "연기 7층",
  stats: { qiBlood: 180, mana: 140, sense: 45, essence: 30, spirit: 140, body: 30, strength: 29 },
};

function statOf(label: string): string {
  return within(screen.getByRole("table", { name: "스탯" })).getByRole("row", { name: new RegExp(`^${label}`) })
    .lastElementChild!.textContent!;
}

beforeEach(() => {
  vi.clearAllMocks();
});

describe("CharacterRealmForm", () => {
  it("지금 경지를 고른 채로 열리고 안내 문구와 스탯 표를 보인다", () => {
    render(<CharacterRealmForm character={garam} realms={realms} onSaved={vi.fn()} />);

    expect(screen.getByLabelText("경지")).toHaveValue("QI_1");
    expect(
      screen.getByText("수위·돌파 없이 경지를 바로 바꿉니다. 스탯은 새 경지 값이 되고 바로 다 찹니다."),
    ).toBeInTheDocument();
    expect(statOf("기혈 최대치")).toBe("60");
    expect(statOf("법력 최대치")).toBe("20");
    expect(statOf("의식 최대치")).toBe("15");
    expect(statOf("정혈 최대치")).toBe("10");
    expect(statOf("영력 최대치")).toBe("20");
    expect(statOf("육신")).toBe("12");
    expect(statOf("근력")).toBe("11");
  });

  it("경지 지정 은 확인 없이 PUT 본문 { realm } 을 보내고, 받은 상세로 스탯 표를 다시 그린다", async () => {
    const user = userEvent.setup();
    const confirm = vi.spyOn(window, "confirm");
    const onSaved = vi.fn();
    vi.mocked(api.designateRealm).mockResolvedValue(qi7);
    render(<CharacterRealmForm character={garam} realms={realms} onSaved={onSaved} />);

    await user.selectOptions(screen.getByLabelText("경지"), "QI_7");
    await user.click(screen.getByRole("button", { name: "경지 지정" }));

    expect(confirm).not.toHaveBeenCalled();
    expect(api.designateRealm).toHaveBeenCalledWith(3, "QI_7");
    expect(await screen.findByRole("status")).toHaveTextContent("경지를 지정했습니다.");
    expect(statOf("기혈 최대치")).toBe("180");
    expect(statOf("근력")).toBe("29");
    expect(onSaved).toHaveBeenCalledWith(qi7);
  });

  it("실패하면 서버의 detail 을 보이고 스탯 표는 그대로다", async () => {
    const user = userEvent.setup();
    const onSaved = vi.fn();
    vi.mocked(api.designateRealm).mockRejectedValue(new api.ApiError(400, "캐릭터를 찾을 수 없습니다. id: 3"));
    render(<CharacterRealmForm character={garam} realms={realms} onSaved={onSaved} />);

    await user.selectOptions(screen.getByLabelText("경지"), "QI_7");
    await user.click(screen.getByRole("button", { name: "경지 지정" }));

    expect(await screen.findByRole("alert")).toHaveTextContent("캐릭터를 찾을 수 없습니다. id: 3");
    expect(screen.queryByRole("status")).not.toBeInTheDocument();
    expect(statOf("기혈 최대치")).toBe("60");
    expect(onSaved).not.toHaveBeenCalled();
  });
});

describe("designateRealm 본문", () => {
  it("PUT /api/admin/characters/{id}/realm 본문은 {realm:\"QI_7\"} 이다", async () => {
    const { designateRealm } = await vi.importActual<typeof import("../lib/api")>("../lib/api");
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(JSON.stringify(qi7), { status: 200, headers: { "Content-Type": "application/json" } }),
    );
    vi.stubGlobal("fetch", fetchMock);
    try {
      await designateRealm(3, "QI_7");
    } finally {
      vi.unstubAllGlobals();
    }

    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toMatch(/\/api\/admin\/characters\/3\/realm$/);
    expect(init.method).toBe("PUT");
    expect(JSON.parse(init.body as string)).toEqual({ realm: "QI_7" });
  });
});

describe("CharacterTable", () => {
  it("영근이 없으면 미정, 접속 중이 아니면 - 를 보이고 이름은 상세 링크다", () => {
    render(
      <CharacterTable
        characters={[
          {
            id: 3,
            name: "가람",
            realm: "QI_1",
            realmName: "연기 1층",
            spiritRootName: "목·화 진영근",
            techniqueCount: 1,
            online: true,
          },
          {
            id: 4,
            name: "여림",
            realm: "MORTAL",
            realmName: "범인",
            spiritRootName: null,
            techniqueCount: 0,
            online: false,
          },
        ]}
      />,
    );

    const rows = screen.getAllByRole("row").slice(1);
    expect(rows.map((row) => [...row.querySelectorAll("td")].map((td) => td.textContent))).toEqual([
      ["가람", "연기 1층", "목·화 진영근", "1", "접속 중"],
      ["여림", "범인", "미정", "0", "-"],
    ]);
    expect(screen.getByRole("link", { name: "가람" }).getAttribute("href")).toMatch(/^\/character\/?\?id=3$/);
  });

  it("캐릭터가 없으면 안내를 보인다", () => {
    render(<CharacterTable characters={[]} />);

    expect(screen.getByText("아직 캐릭터가 없습니다.")).toBeInTheDocument();
  });
});

describe("CharacterDetail", () => {
  it("이름·영근(자질 소수 첫째 자리)·익힌 공법(공법 링크, 1/9성)을 보인다", () => {
    render(<CharacterDetail initial={garam} realms={realms} />);

    expect(screen.getByRole("heading", { name: "가람" })).toBeInTheDocument();
    expect(screen.getByText("영근: 목·화 진영근 (목 50, 화 52.3)")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "장춘공" }).getAttribute("href")).toMatch(/^\/technique\/?\?id=2$/);
    expect(screen.getByText("1/9성")).toBeInTheDocument();
  });

  it("영근이 없으면 아직 정하지 않음, 익힌 공법이 없으면 없음 을 보인다", () => {
    render(<CharacterDetail initial={{ ...garam, spiritRoot: null, techniques: [] }} realms={realms} />);

    expect(screen.getByText("영근: 아직 정하지 않음")).toBeInTheDocument();
    expect(screen.getByText("없음")).toBeInTheDocument();
    expect(screen.queryByRole("link")).not.toBeInTheDocument();
  });
});
