// Types and helpers for the 1:1 interview call (WebRTC over the collab signaling WS).

export type CallPeer = {
  userId: string;
  email: string;
  role: string;
};

/** Shared interview phase, driven by the interviewer. */
export type CallPhase = "lobby" | "coding";

// ─── Signaling messages (server → client) ──────────────────────────
export type JoinedMessage = {
  type: "joined";
  you: CallPeer;
  phase: CallPhase;
  peers: CallPeer[];
};

export type PeerJoinedMessage = { type: "peer-joined"; from: CallPeer };
export type PeerLeftMessage = { type: "peer-left"; from: CallPeer };

export type SdpMessage = {
  type: "sdp";
  description: RTCSessionDescriptionInit;
  from: CallPeer;
};

export type IceMessage = {
  type: "ice";
  candidate: RTCIceCandidateInit;
  from: CallPeer;
};

export type PhaseMessage = { type: "phase"; phase: CallPhase; from: CallPeer };

export type MediaMessage = {
  type: "media";
  mic: boolean;
  cam: boolean;
  screen: boolean;
  from: CallPeer;
};

export type CallServerMessage =
  | JoinedMessage
  | PeerJoinedMessage
  | PeerLeftMessage
  | SdpMessage
  | IceMessage
  | PhaseMessage
  | MediaMessage;

/** State of the remote peer's tracks, for showing mic/cam indicators. */
export type RemoteMediaState = {
  mic: boolean;
  cam: boolean;
  screen: boolean;
};

// Public STUN keeps host candidates reachable across the loopback/LAN; for a
// single-machine demo even this is optional. No TURN needed for 1:1 on localhost.
export const ICE_SERVERS: RTCIceServer[] = [
  { urls: "stun:stun.l.google.com:19302" },
];

export function buildCallUrl(sessionId: string, token: string): string {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws/calls/${sessionId}?token=${encodeURIComponent(token)}`;
}
