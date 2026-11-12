export type DirectionCode =
  | "EAST"
  | "WEST"
  | "SOUTH"
  | "NORTH"
  | "NORTHEAST"
  | "NORTHWEST"
  | "SOUTHEAST"
  | "SOUTHWEST"
  | "UP"
  | "DOWN";

export type DirectionName = "동" | "서" | "남" | "북" | "북동" | "북서" | "남동" | "남서" | "위" | "아래";

export const DIRECTIONS: { code: DirectionCode; name: DirectionName }[] = [
  { code: "EAST", name: "동" },
  { code: "WEST", name: "서" },
  { code: "SOUTH", name: "남" },
  { code: "NORTH", name: "북" },
  { code: "NORTHEAST", name: "북동" },
  { code: "NORTHWEST", name: "북서" },
  { code: "SOUTHEAST", name: "남동" },
  { code: "SOUTHWEST", name: "남서" },
  { code: "UP", name: "위" },
  { code: "DOWN", name: "아래" },
];
