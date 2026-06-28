import { useEffect, useRef, useState } from "react";
import {
  Mic,
  MicOff,
  Video,
  VideoOff,
  PhoneCall,
  MessagesSquare,
  Code2,
  ArrowRight,
  Loader2,
  AlertTriangle,
} from "lucide-react";
import { useAuthStore } from "@/stores/auth";
import type { Session } from "@/types/api";
import { useCall } from "@/hooks/useCall";
import { CallPanel, VideoTile } from "@/components/call/CallPanel";
import { CollabRoom, type CollabRoomHandle } from "@/components/CollabRoom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";

type View = "call" | "code";

interface InterviewRoomProps {
  session: Session;
  collabRef: React.MutableRefObject<CollabRoomHandle | null>;
}

/**
 * Live interview workspace for an IN_PROGRESS session: a persistent 1:1 call
 * plus a coding section the interviewer unlocks. The call (RTCPeerConnection +
 * media) lives here and is never torn down when switching views, so audio/video
 * keep flowing whether you're on the "Разговор" or "Секция кода" tab.
 */
export function InterviewRoom({ session, collabRef }: InterviewRoomProps) {
  const user = useAuthStore((s) => s.user);
  const isController =
    user?.role === "ADMIN" || (user?.role === "INTERVIEWER" && user.id === session.interviewerId);

  const call = useCall({ sessionId: session.id, isController });
  const { status, phase, localStream, micOn, camOn } = call;

  const inCall = status === "connecting" || status === "in-call";
  const [view, setView] = useState<View>("call");
  const [codeMounted, setCodeMounted] = useState(false);
  const prevPhaseRef = useRef(phase);

  // Show the device preview as soon as the room opens.
  useEffect(() => {
    call.prepareMedia().catch(() => {
      /* permission errors surface via status === "error" */
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // When the interviewer opens the coding section, mount it and jump there once.
  useEffect(() => {
    if (phase === "coding") {
      setCodeMounted(true);
      if (prevPhaseRef.current !== "coding") setView("code");
    }
    prevPhaseRef.current = phase;
  }, [phase]);

  // ── Lobby (before joining the call) ──
  if (!inCall) {
    return (
      <Card>
        <CardContent className="py-8">
          <div className="mx-auto max-w-md space-y-5 text-center">
            <div>
              <h2 className="text-xl font-semibold">Подключение к интервью</h2>
              <p className="mt-1 text-sm text-muted-foreground">
                Проверьте камеру и микрофон, затем войдите в звонок.
              </p>
            </div>

            <div className="relative mx-auto overflow-hidden rounded-xl border bg-black" style={{ aspectRatio: "16 / 9" }}>
              {localStream ? (
                <VideoTile stream={localStream} muted mirror className="absolute inset-0 h-full w-full" />
              ) : status === "error" ? (
                <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 px-4 text-muted-foreground">
                  <AlertTriangle className="h-6 w-6" />
                  <span className="text-sm">Нет доступа к камере/микрофону. Разрешите доступ в браузере.</span>
                </div>
              ) : (
                <div className="absolute inset-0 flex items-center justify-center text-muted-foreground">
                  <Loader2 className="h-6 w-6 animate-spin" />
                </div>
              )}
            </div>

            <div className="flex items-center justify-center gap-2">
              <Button variant={micOn ? "secondary" : "outline"} size="sm" onClick={call.toggleMic} disabled={!localStream}>
                {micOn ? <Mic className="h-4 w-4" /> : <MicOff className="h-4 w-4" />}
                {micOn ? "Микрофон вкл" : "Микрофон выкл"}
              </Button>
              <Button variant={camOn ? "secondary" : "outline"} size="sm" onClick={call.toggleCam} disabled={!localStream}>
                {camOn ? <Video className="h-4 w-4" /> : <VideoOff className="h-4 w-4" />}
                {camOn ? "Камера вкл" : "Камера выкл"}
              </Button>
            </div>

            {status === "error" ? (
              <Button onClick={() => call.prepareMedia().catch(() => {})}>Повторить доступ</Button>
            ) : (
              <Button size="lg" onClick={() => call.connect()} disabled={status === "preparing"}>
                <PhoneCall className="h-4 w-4" />
                Войти в звонок
              </Button>
            )}

            {!isController && (
              <p className="text-xs text-muted-foreground">
                Интервьюер пригласит вас в секцию кода после короткого разговора.
              </p>
            )}
          </div>
        </CardContent>
      </Card>
    );
  }

  // ── In call ──
  const codingUnlocked = phase === "coding";

  return (
    <div className="space-y-4">
      {/* View switcher / phase control */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {codingUnlocked ? (
          <div className="flex rounded-md border p-0.5">
            <TabButton active={view === "call"} onClick={() => setView("call")} icon={MessagesSquare}>
              Разговор
            </TabButton>
            <TabButton active={view === "code"} onClick={() => setView("code")} icon={Code2}>
              Секция кода
            </TabButton>
          </div>
        ) : (
          <div className="text-sm text-muted-foreground">
            {isController ? "Разговор. Откройте секцию кода, когда будете готовы." : "Идёт разговор с интервьюером."}
          </div>
        )}

        {isController && !codingUnlocked && (
          <Button onClick={() => call.setPhase("coding")}>
            Пригласить на тех. секцию
            <ArrowRight className="h-4 w-4" />
          </Button>
        )}
      </div>

      {/* Call view (hidden, not unmounted, when on the code tab so media keeps flowing) */}
      <div className={codingUnlocked && view === "code" ? "hidden" : ""}>
        <CallPanel call={call} />
      </div>

      {/* Coding section — mounted once unlocked, kept alive across view switches */}
      {codeMounted && (
        <>
          <div className={view === "code" ? "" : "hidden"}>
            <CollabRoom ref={collabRef} session={session} />
          </div>
          {view === "code" && <CallPanel call={call} compact />}
        </>
      )}
    </div>
  );
}

function TabButton({
  active,
  onClick,
  icon: Icon,
  children,
}: {
  active: boolean;
  onClick: () => void;
  icon: typeof Code2;
  children: React.ReactNode;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex items-center gap-1.5 rounded px-3 py-1.5 text-sm font-medium transition-colors ${
        active ? "bg-secondary text-secondary-foreground" : "text-muted-foreground hover:text-foreground"
      }`}
    >
      <Icon className="h-4 w-4" />
      {children}
    </button>
  );
}
