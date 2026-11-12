"use client";

import { type FormEvent, useState } from "react";
import { errorMessage, type RoomSaveRequest, type ZoneResponse } from "../lib/api";

type Props = {
  zones: ZoneResponse[];
  submitLabel: string;
  /** 있으면 고치는 폼, 없으면 새로 만드는 폼(성공하면 이름·묘사를 비운다) */
  initial?: RoomSaveRequest;
  defaultZoneId?: number;
  onSubmit: (request: RoomSaveRequest) => Promise<void>;
};

export default function RoomForm({ zones, submitLabel, initial, defaultZoneId, onSubmit }: Props) {
  const [zoneId, setZoneId] = useState<number | null>(initial?.zoneId ?? defaultZoneId ?? null);
  const [name, setName] = useState(initial?.name ?? "");
  const [description, setDescription] = useState(initial?.description ?? "");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const filled = zoneId !== null && name.trim() !== "" && description.trim() !== "";

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (zoneId === null) {
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      await onSubmit({ zoneId, name: name.trim(), description });
      if (!initial) {
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
        지역
        <select value={zoneId ?? ""} required onChange={(e) => setZoneId(e.target.value ? Number(e.target.value) : null)}>
          <option value="">지역 선택</option>
          {zones.map((zone) => (
            <option key={zone.id} value={zone.id}>
              {zone.name}
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
        <textarea
          value={description}
          maxLength={1000}
          rows={5}
          required
          onChange={(e) => setDescription(e.target.value)}
        />
      </label>
      <button type="submit" disabled={submitting || !filled}>
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
