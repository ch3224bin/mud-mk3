"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import AdminGuard from "../../src/components/AdminGuard";
import TopBar from "../../src/components/TopBar";
import { errorMessage, type ItemTypeResponse, listItemTypes } from "../../src/lib/api";
import { formatCount } from "../../src/lib/items";

export default function ItemsPage() {
  return (
    <AdminGuard>
      <ItemsView />
    </AdminGuard>
  );
}

/** 겹침 칸에 보일 값. 저물대는 칸을 차지하지 않으므로 대신 주는 칸 수를 보인다 */
function stackLabel(itemType: ItemTypeResponse): string {
  return itemType.category === "BAG" ? `${formatCount(itemType.bagSlots)}칸` : formatCount(itemType.stackSize);
}

function ItemsView() {
  const [itemTypes, setItemTypes] = useState<ItemTypeResponse[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listItemTypes()
      .then(setItemTypes)
      .catch((e) => setError(errorMessage(e)));
  }, []);

  return (
    <>
      <TopBar />
      <main>
        <h1>아이템</h1>
        <p>
          <Link className="button" href="/item/">
            아이템 만들기
          </Link>
        </p>
        {error && (
          <p role="alert" className="error">
            {error}
          </p>
        )}
        {itemTypes === null ? (
          !error && <p className="muted">불러오는 중…</p>
        ) : itemTypes.length === 0 ? (
          <p className="muted">아직 아이템이 없습니다.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>이름</th>
                <th>종류</th>
                <th>기준가</th>
                <th>겹침</th>
                <th>거래</th>
              </tr>
            </thead>
            <tbody>
              {itemTypes.map((itemType) => (
                <tr key={itemType.id}>
                  <td>
                    <Link href={`/item/?id=${itemType.id}`}>{itemType.name}</Link>
                  </td>
                  <td>{itemType.categoryName}</td>
                  <td>{formatCount(itemType.baseValue)}</td>
                  <td>{stackLabel(itemType)}</td>
                  <td>{itemType.tradable ? "가능" : "불가"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </>
  );
}
