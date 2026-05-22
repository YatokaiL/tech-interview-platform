export type Presence = {
  userId: string;
  email: string;
  role: string;
};

export type InitMessage = {
  type: "init";
  code: string;
  language: string;
  you: Presence;
  participants: Presence[];
};

export type EditMessage = {
  type: "edit";
  code: string;
  language?: string;
  by: Presence;
  at: string;
};

export type CursorMessage = {
  type: "cursor";
  line: number;
  column: number;
  by: Presence;
};

export type ChatMessage = {
  type: "chat";
  text: string;
  by: Presence;
  at: string;
};

export type ParticipantJoinedMessage = {
  type: "participant-joined";
  userId: string;
  email: string;
  role: string;
};

export type ParticipantLeftMessage = {
  type: "participant-left";
  userId: string;
  email: string;
  role: string;
};

export type ErrorMessage = {
  type: "error";
  message: string;
};

export type ServerMessage =
  | InitMessage
  | EditMessage
  | CursorMessage
  | ChatMessage
  | ParticipantJoinedMessage
  | ParticipantLeftMessage
  | ErrorMessage;

export function buildCollabUrl(sessionId: string, token: string): string {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws/sessions/${sessionId}?token=${encodeURIComponent(token)}`;
}
