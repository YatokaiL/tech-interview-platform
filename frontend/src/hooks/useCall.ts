import { useCallback, useEffect, useRef, useState } from "react";
import { useAuthStore } from "@/stores/auth";
import {
  ICE_SERVERS,
  buildCallUrl,
  type CallPeer,
  type CallPhase,
  type CallServerMessage,
  type RemoteMediaState,
} from "@/lib/call";

export type CallStatus = "idle" | "preparing" | "connecting" | "in-call" | "error";

interface UseCallOptions {
  sessionId: string;
  /** Whether this user may drive the shared phase (interviewer / admin). */
  isController: boolean;
}

/**
 * Manages a 1:1 WebRTC call for an interview: local/remote media, the signaling
 * WebSocket and the shared phase. Uses the "perfect negotiation" pattern so the
 * two peers can offer simultaneously without glare; screen-share swaps the
 * outgoing video track in place (replaceTrack) and needs no renegotiation.
 */
export function useCall({ sessionId, isController }: UseCallOptions) {
  const token = useAuthStore((s) => s.accessToken);
  const myId = useAuthStore((s) => s.user?.id) ?? "";

  const [status, setStatus] = useState<CallStatus>("idle");
  const [localStream, setLocalStream] = useState<MediaStream | null>(null);
  const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);
  const [micOn, setMicOn] = useState(true);
  const [camOn, setCamOn] = useState(true);
  const [screenOn, setScreenOn] = useState(false);
  const [remoteMedia, setRemoteMedia] = useState<RemoteMediaState>({ mic: false, cam: false, screen: false });
  const [remotePeer, setRemotePeer] = useState<CallPeer | null>(null);
  const [phase, setPhaseState] = useState<CallPhase>("lobby");

  const wsRef = useRef<WebSocket | null>(null);
  const pcRef = useRef<RTCPeerConnection | null>(null);
  const localStreamRef = useRef<MediaStream | null>(null);
  const screenStreamRef = useRef<MediaStream | null>(null);
  const cameraTrackRef = useRef<MediaStreamTrack | null>(null);
  const pendingCandidatesRef = useRef<RTCIceCandidateInit[]>([]);
  const micRef = useRef(true);
  const camRef = useRef(true);
  const screenRef = useRef(false);

  const wsSend = useCallback((obj: unknown) => {
    const ws = wsRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj));
  }, []);

  const sendMediaState = useCallback(() => {
    wsSend({ type: "media", mic: micRef.current, cam: camRef.current, screen: screenRef.current });
  }, [wsSend]);

  // Acquire camera + mic for the lobby preview (idempotent).
  const prepareMedia = useCallback(async () => {
    if (localStreamRef.current) return localStreamRef.current;
    setStatus("preparing");
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true, video: true });
      localStreamRef.current = stream;
      cameraTrackRef.current = stream.getVideoTracks()[0] ?? null;
      setLocalStream(stream);
      setStatus("idle");
      return stream;
    } catch (e) {
      console.error("getUserMedia failed", e);
      setStatus("error");
      throw e;
    }
  }, []);

  // Deterministic 1:1 initiator: the peer with the larger userId sends the offer,
  // regardless of who joined first — no glare, no lost offers.
  const makeOffer = useCallback(async () => {
    const pc = pcRef.current;
    if (!pc) return;
    try {
      const offer = await pc.createOffer();
      await pc.setLocalDescription(offer);
      wsSend({ type: "sdp", description: pc.localDescription });
    } catch (e) {
      console.error("makeOffer failed", e);
    }
  }, [wsSend]);

  const maybeOffer = useCallback(
    (peer: CallPeer) => {
      setRemotePeer(peer);
      if (myId > peer.userId) void makeOffer();
    },
    [myId, makeOffer]
  );

  const handleSignal = useCallback(
    async (msg: CallServerMessage) => {
      const pc = pcRef.current;
      switch (msg.type) {
        case "joined": {
          setPhaseState(msg.phase);
          if (msg.peers.length > 0) maybeOffer(msg.peers[0]);
          break;
        }
        case "peer-joined": {
          maybeOffer(msg.from);
          // Let the newcomer know our current mic/cam/screen state.
          sendMediaState();
          break;
        }
        case "peer-left": {
          setRemotePeer(null);
          setRemoteStream(null);
          setRemoteMedia({ mic: false, cam: false, screen: false });
          pendingCandidatesRef.current = [];
          break;
        }
        case "sdp": {
          if (!pc) return;
          await pc.setRemoteDescription(msg.description);
          // Flush any ICE candidates that arrived before the remote description.
          for (const c of pendingCandidatesRef.current) {
            try {
              await pc.addIceCandidate(c);
            } catch (e) {
              console.error("addIceCandidate (queued) failed", e);
            }
          }
          pendingCandidatesRef.current = [];
          if (msg.description.type === "offer") {
            const answer = await pc.createAnswer();
            await pc.setLocalDescription(answer);
            wsSend({ type: "sdp", description: pc.localDescription });
          }
          break;
        }
        case "ice": {
          if (!pc) return;
          if (pc.remoteDescription) {
            try {
              await pc.addIceCandidate(msg.candidate);
            } catch (e) {
              console.error("addIceCandidate failed", e);
            }
          } else {
            pendingCandidatesRef.current.push(msg.candidate);
          }
          break;
        }
        case "phase": {
          setPhaseState(msg.phase);
          break;
        }
        case "media": {
          setRemoteMedia({ mic: msg.mic, cam: msg.cam, screen: msg.screen });
          break;
        }
      }
    },
    [wsSend, sendMediaState, maybeOffer]
  );

  // Join the call: build the peer connection, attach local tracks, open signaling.
  const connect = useCallback(async () => {
    if (!token || pcRef.current) return;
    const stream = await prepareMedia();
    setStatus("connecting");

    const pc = new RTCPeerConnection({ iceServers: ICE_SERVERS });
    pcRef.current = pc;
    stream.getTracks().forEach((t) => pc.addTrack(t, stream));

    pc.onicecandidate = ({ candidate }) => {
      if (candidate) wsSend({ type: "ice", candidate: candidate.toJSON() });
    };
    pc.ontrack = ({ streams }) => {
      setRemoteStream(streams[0] ?? null);
    };
    pc.onconnectionstatechange = () => {
      if (pc.connectionState === "connected") setStatus("in-call");
      else if (pc.connectionState === "failed") setStatus("error");
    };

    const ws = new WebSocket(buildCallUrl(sessionId, token));
    wsRef.current = ws;
    ws.onopen = () => {
      setStatus("in-call");
      sendMediaState();
    };
    ws.onmessage = (e) => {
      let msg: CallServerMessage;
      try {
        msg = JSON.parse(e.data);
      } catch {
        return;
      }
      void handleSignal(msg);
    };
    ws.onerror = () => setStatus("error");
    ws.onclose = () => {
      setRemotePeer(null);
      setRemoteStream(null);
    };
  }, [token, sessionId, prepareMedia, wsSend, handleSignal, sendMediaState]);

  const cleanup = useCallback(() => {
    wsRef.current?.close();
    wsRef.current = null;
    pcRef.current?.close();
    pcRef.current = null;
    screenStreamRef.current?.getTracks().forEach((t) => t.stop());
    screenStreamRef.current = null;
    localStreamRef.current?.getTracks().forEach((t) => t.stop());
    localStreamRef.current = null;
    cameraTrackRef.current = null;
  }, []);

  const leave = useCallback(() => {
    cleanup();
    setLocalStream(null);
    setRemoteStream(null);
    setRemotePeer(null);
    setStatus("idle");
  }, [cleanup]);

  // Stop everything when the component using the call unmounts.
  useEffect(() => cleanup, [cleanup]);

  const toggleMic = useCallback(() => {
    const track = localStreamRef.current?.getAudioTracks()[0];
    if (!track) return;
    track.enabled = !track.enabled;
    micRef.current = track.enabled;
    setMicOn(track.enabled);
    sendMediaState();
  }, [sendMediaState]);

  const toggleCam = useCallback(() => {
    const track = localStreamRef.current?.getVideoTracks()[0];
    if (!track) return;
    track.enabled = !track.enabled;
    camRef.current = track.enabled;
    setCamOn(track.enabled);
    sendMediaState();
  }, [sendMediaState]);

  const stopScreen = useCallback(() => {
    const pc = pcRef.current;
    screenStreamRef.current?.getTracks().forEach((t) => t.stop());
    screenStreamRef.current = null;
    const sender = pc?.getSenders().find((s) => s.track?.kind === "video");
    if (sender && cameraTrackRef.current) void sender.replaceTrack(cameraTrackRef.current);
    screenRef.current = false;
    setScreenOn(false);
    sendMediaState();
  }, [sendMediaState]);

  const toggleScreen = useCallback(async () => {
    if (screenRef.current) {
      stopScreen();
      return;
    }
    try {
      const screen = await navigator.mediaDevices.getDisplayMedia({ video: true });
      screenStreamRef.current = screen;
      const screenTrack = screen.getVideoTracks()[0];
      const sender = pcRef.current?.getSenders().find((s) => s.track?.kind === "video");
      if (sender) await sender.replaceTrack(screenTrack);
      screenTrack.onended = () => stopScreen(); // user stopped sharing via the browser UI
      screenRef.current = true;
      setScreenOn(true);
      sendMediaState();
    } catch (e) {
      console.error("getDisplayMedia failed", e);
    }
  }, [stopScreen, sendMediaState]);

  const setPhase = useCallback(
    (p: CallPhase) => {
      if (!isController) return;
      setPhaseState(p);
      wsSend({ type: "phase", phase: p });
    },
    [isController, wsSend]
  );

  return {
    status,
    localStream,
    remoteStream,
    micOn,
    camOn,
    screenOn,
    remoteMedia,
    remotePeer,
    phase,
    prepareMedia,
    connect,
    leave,
    toggleMic,
    toggleCam,
    toggleScreen,
    setPhase,
  };
}

export type CallController = ReturnType<typeof useCall>;
