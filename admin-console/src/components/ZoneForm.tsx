"use client";

import { type FormEvent, useState } from "react";
import { errorMessage, type FactionResponse, type ZoneSaveRequest } from "../lib/api";
import { factionLabel } from "../lib/factions";

type Props = {
  submitLabel: string;
  /** 소유로 고를 수 있는 성·종문 */
  factions?: FactionResponse[];
  /** 있으면 고치는 폼, 없으면 새로 만드는 폼(성공하면 비운다) */
  initial?: ZoneSaveRequest;
  onSubmit: (request: ZoneSaveRequest) => Promise<void>;
};

export default function ZoneForm({ submitLabel, factions = [], initial, onSubmit }: Props) {
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [ownerId, setOwnerId] = useState<number | null>(initial?.ownerId ?? null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({ name: name.trim(), description, ownerId });
      if (!initial) {
        setName("");
        setDescription("");
        setOwnerId(null);
      }
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form className="form" onSubmit={handleSubmit}>
      <label>
        이름
        <input value={name} maxLength={30} required onChange={(e) => setName(e.target.value)} />
      </label>
      <label>
        분위기 묘사
        <textarea value={description} maxLength={500} rows={3} onChange={(e) => setDescription(e.target.value)} />
      </label>
      <label>
        소유
        <select value={ownerId ?? ""} onChange={(e) => setOwnerId(e.target.value ? Number(e.target.value) : null)}>
          <option value="">없음</option>
          {factions.map((faction) => (
            <option key={faction.id} value={faction.id}>
              {factionLabel(faction.name, faction.kind)}
            </option>
          ))}
        </select>
      </label>
      <button type="submit" disabled={submitting || name.trim() === ""}>
        {submitLabel}
      </button>
      {error && (
        <p role="alert" className="error">
          {error}
        </p>
      )}
    </form>
  );
}
