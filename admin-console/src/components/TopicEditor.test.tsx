import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useState } from "react";
import { describe, expect, it } from "vitest";
import type { NpcTopic } from "../lib/api";
import TopicEditor, { withoutEmptyRows } from "./TopicEditor";

function Harness({ initial }: { initial: NpcTopic[] }) {
  const [topics, setTopics] = useState(initial);
  return <TopicEditor topics={topics} onChange={setTopics} />;
}

describe("TopicEditor", () => {
  it("낱말 추가 를 누르면 빈 행이 생긴다", async () => {
    const user = userEvent.setup();
    render(<Harness initial={[]} />);

    expect(screen.queryAllByLabelText("낱말")).toHaveLength(0);
    await user.click(screen.getByRole("button", { name: "낱말 추가" }));

    expect(screen.getAllByLabelText("낱말")).toHaveLength(1);
    expect(screen.getAllByLabelText("대답")).toHaveLength(1);
    await user.type(screen.getByLabelText("낱말"), "단약");
    expect(screen.getByLabelText("낱말")).toHaveValue("단약");
  });

  it("행의 삭제 를 누르면 그 행만 사라진다", async () => {
    const user = userEvent.setup();
    render(
      <Harness
        initial={[
          { word: "단약", answer: "단약은 귀하오." },
          { word: "마을", answer: "조용한 곳이오." },
        ]}
      />,
    );

    await user.click(screen.getAllByRole("button", { name: "삭제" })[0]);

    const words = screen.getAllByLabelText("낱말");
    expect(words).toHaveLength(1);
    expect(words[0]).toHaveValue("마을");
    expect(screen.getByLabelText("대답")).toHaveValue("조용한 곳이오.");
  });

  it("낱말과 대답이 둘 다 빈 행은 저장할 때 뺀다", () => {
    expect(
      withoutEmptyRows([
        { word: "단약", answer: "단약은 귀하오." },
        { word: "", answer: "" },
        { word: "  ", answer: " " },
        { word: "마을", answer: "" },
      ]),
    ).toEqual([
      { word: "단약", answer: "단약은 귀하오." },
      { word: "마을", answer: "" },
    ]);
  });
});
