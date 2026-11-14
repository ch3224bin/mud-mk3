"use client";

import { type FormEvent, useState } from "react";
import { errorMessage, type FactionKind, type FactionSaveRequest } from "../lib/api";
import { FACTION_KINDS } from "../lib/factions";

type Props = {
  submitLabel: string;
  /** 있으면 고치는 폼, 없으면 새로 만드는 폼(성공하면 비운다) */
  initial?: FactionSaveRequest;
  onSubmit: (request: FactionSaveRequest) => Promise<void>;
};

export default function FactionForm({ submitLabel, initial, onSubmit }: Props) {
  const [kind, setKind] = useState<FactionKind>(initial?.kind ?? "CASTLE");
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({ kind, name: name.trim(), description });
      if (!initial) {
        setKind("CASTLE");
        setName("");
        setDescription("");
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
        종류
        <select value={kind} onChange={(e) => setKind(e.target.value as FactionKind)}>
          {FACTION_KINDS.map((k) => (
            <option key={k.code} value={k.code}>
              {k.name}
            </option>
          ))}
        </select>
      </label>
      <label>
        이름
        <input value={name} maxLength={30} required onChange={(e) => setName(e.target.value)} />
      </label>
      <label>
        묘사
        <textarea value={description} maxLength={500} rows={3} onChange={(e) => setDescription(e.target.value)} />
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
