import { useEffect, useRef } from "react";
import {
  Mic,
  MicOff,
  Video,
  VideoOff,
  ScreenShare,
  ScreenShareOff,
  PhoneOff,
  Loader2,
  MonitorPlay,
} from "lucide-react";
import type { CallController } from "@/hooks/useCall";
import { Button } from "@/components/ui/button";

export function VideoTile({
  stream,
  muted,
  mirror,
  className,
}: {
  stream: MediaStream | null;
  muted?: boolean;
  mirror?: boolean;
  className?: string;
}) {
  const ref = useRef<HTMLVideoElement | null>(null);
  useEffect(() => {
    const el = ref.current;
    if (el && el.srcObject !== stream) el.srcObject = stream;
  }, [stream]);
  return (
    <video
      ref={ref}
      autoPlay
      playsInline
      muted={muted}
      className={`${className ?? ""} ${mirror ? "-scale-x-100" : ""} bg-black object-cover`}
    />
  );
}

function ControlButton({
  on,
  onClick,
  OnIcon,
  OffIcon,
  title,
  danger,
  small,
}: {
  on: boolean;
  onClick: () => void;
  OnIcon: typeof Mic;
  OffIcon: typeof Mic;
  title: string;
  danger?: boolean;
  small?: boolean;
}) {
  const Icon = on ? OnIcon : OffIcon;
  const size = small ? "h-8 w-8" : "h-10 w-10";
  const variant = danger ? "bg-destructive text-destructive-foreground hover:bg-destructive/90" : on
    ? "bg-secondary text-secondary-foreground hover:bg-secondary/80"
    : "bg-muted text-muted-foreground hover:bg-muted/80";
  return (
    <button
      type="button"
      title={title}
      onClick={onClick}
      className={`${size} ${variant} flex items-center justify-center rounded-full transition-colors`}
    >
      <Icon className={small ? "h-4 w-4" : "h-5 w-5"} />
    </button>
  );
}

interface CallPanelProps {
  call: CallController;
  /** Compact picture-in-picture mode (shown while the coding section is active). */
  compact?: boolean;
}

export function CallPanel({ call, compact }: CallPanelProps) {
  const { localStream, remoteStream, micOn, camOn, screenOn, remoteMedia, remotePeer } = call;

  const controls = (small?: boolean) => (
    <div className={`flex items-center justify-center gap-2 ${small ? "" : "py-1"}`}>
      <ControlButton on={micOn} onClick={call.toggleMic} OnIcon={Mic} OffIcon={MicOff} title="Микрофон" small={small} />
      <ControlButton on={camOn} onClick={call.toggleCam} OnIcon={Video} OffIcon={VideoOff} title="Камера" small={small} />
      <ControlButton
        on={screenOn}
        onClick={call.toggleScreen}
        OnIcon={ScreenShareOff}
        OffIcon={ScreenShare}
        title={screenOn ? "Остановить демонстрацию" : "Демонстрация экрана"}
        small={small}
      />
      <ControlButton on={false} onClick={call.leave} OnIcon={PhoneOff} OffIcon={PhoneOff} title="Покинуть звонок" danger small={small} />
    </div>
  );

  const remoteLabel = remotePeer?.email ?? "Собеседник";

  // ── Compact PiP (floats over the coding section) ──
  if (compact) {
    return (
      <div className="fixed bottom-4 right-4 z-50 w-64 overflow-hidden rounded-lg border bg-card shadow-xl">
        <div className="relative aspect-video">
          {remoteStream ? (
            <VideoTile stream={remoteStream} className="absolute inset-0 h-full w-full" />
          ) : (
            <div className="absolute inset-0 flex items-center justify-center bg-muted text-xs text-muted-foreground">
              Ожидание собеседника…
            </div>
          )}
          {!remoteMedia.mic && remoteStream && (
            <span className="absolute left-1.5 top-1.5 rounded bg-black/60 p-1">
              <MicOff className="h-3 w-3 text-white" />
            </span>
          )}
          <div className="absolute bottom-1 left-1.5 rounded bg-black/60 px-1.5 py-0.5 text-[10px] text-white">
            {remoteLabel}
          </div>
          <VideoTile
            stream={localStream}
            muted
            mirror
            className="absolute bottom-1 right-1 h-12 w-16 rounded border border-white/30"
          />
        </div>
        <div className="border-t p-1.5">{controls(true)}</div>
      </div>
    );
  }

  // ── Full call stage ──
  return (
    <div className="space-y-3">
      <div className="relative overflow-hidden rounded-xl border bg-black" style={{ aspectRatio: "16 / 9" }}>
        {remoteStream ? (
          <VideoTile stream={remoteStream} className="absolute inset-0 h-full w-full" />
        ) : (
          <div className="absolute inset-0 flex flex-col items-center justify-center gap-2 text-muted-foreground">
            <Loader2 className="h-6 w-6 animate-spin" />
            <span className="text-sm">Ожидание собеседника…</span>
          </div>
        )}

        {remoteStream && (
          <div className="absolute left-3 top-3 flex items-center gap-2 rounded bg-black/60 px-2 py-1 text-xs text-white">
            <span>{remoteLabel}</span>
            {!remoteMedia.mic && <MicOff className="h-3.5 w-3.5" />}
            {remoteMedia.screen && <MonitorPlay className="h-3.5 w-3.5" />}
          </div>
        )}

        {/* self preview */}
        <div className="absolute bottom-3 right-3 w-40 overflow-hidden rounded-lg border-2 border-white/40 shadow-lg sm:w-48">
          <div className="relative" style={{ aspectRatio: "16 / 9" }}>
            <VideoTile stream={localStream} muted mirror className="absolute inset-0 h-full w-full" />
            {!camOn && (
              <div className="absolute inset-0 flex items-center justify-center bg-muted text-xs text-muted-foreground">
                Камера выключена
              </div>
            )}
            <div className="absolute bottom-1 left-1.5 rounded bg-black/60 px-1.5 py-0.5 text-[10px] text-white">
              Вы {!micOn && "🔇"}
            </div>
          </div>
        </div>
      </div>

      {controls()}
    </div>
  );
}
