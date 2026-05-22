import {
  forwardRef,
  useCallback,
  useEffect,
  useImperativeHandle,
  useRef,
  useState,
} from "react";
import Editor from "@monaco-editor/react";
import { Loader2, Save, Send, Users, Wifi, WifiOff } from "lucide-react";
import { toast } from "sonner";
import { isAxiosError } from "axios";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth";
import { monacoLanguage, ROLE_LABELS } from "@/lib/roles";
import type {
  ApiError,
  CodeSnapshot,
  CodeSnapshotRequest,
  Session,
  SessionEvent,
} from "@/types/api";
import { buildCollabUrl, type Presence, type ServerMessage } from "@/lib/collab";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";

type ConnState = "idle" | "connecting" | "open" | "closed" | "error";

interface ChatEntry {
  id: number;
  by: Presence;
  text: string;
  at: string;
}

interface CollabRoomProps {
  session: Session;
}

export interface CollabRoomHandle {
  flushSnapshot: (type?: "AUTO" | "MANUAL" | "FINAL") => Promise<void>;
}

const EDIT_DEBOUNCE_MS = 250;
const AUTOSAVE_INTERVAL_MS = 30_000;
const MAX_CHAT_ENTRIES = 200;

export const CollabRoom = forwardRef<CollabRoomHandle, CollabRoomProps>(function CollabRoom(
  { session },
  ref
) {
  const user = useAuthStore((s) => s.user);
  const token = useAuthStore((s) => s.accessToken);

  const canEdit =
    session.status === "IN_PROGRESS" &&
    (user?.role === "ADMIN" ||
      user?.id === session.interviewerId ||
      user?.id === session.candidateId);

  const isOpenSession = session.status === "SCHEDULED" || session.status === "IN_PROGRESS";

  const [code, setCode] = useState<string>("");
  const [language, setLanguage] = useState<string>(session.language);
  const [conn, setConn] = useState<ConnState>("idle");
  const [participants, setParticipants] = useState<Presence[]>([]);
  const [chat, setChat] = useState<ChatEntry[]>([]);
  const [chatInput, setChatInput] = useState("");
  const [loadingSnapshot, setLoadingSnapshot] = useState(true);
  const [savingSnapshot, setSavingSnapshot] = useState(false);
  const [lastSavedAt, setLastSavedAt] = useState<Date | null>(null);

  const wsRef = useRef<WebSocket | null>(null);
  const debounceRef = useRef<number | null>(null);
  const remoteUpdateRef = useRef(false);
  const dirtyRef = useRef(false);
  const codeRef = useRef<string>("");
  const languageRef = useRef<string>(session.language);
  const chatIdRef = useRef(0);

  codeRef.current = code;
  languageRef.current = language;

  const pushChat = useCallback((entry: Omit<ChatEntry, "id">) => {
    setChat((prev) => {
      const next = [...prev, { ...entry, id: ++chatIdRef.current }];
      return next.length > MAX_CHAT_ENTRIES ? next.slice(-MAX_CHAT_ENTRIES) : next;
    });
  }, []);

  // Load chat history once on mount
  useEffect(() => {
    let cancelled = false;
    api
      .get<SessionEvent[]>(`/sessions/${session.id}/events`, {
        params: { type: "chat", limit: 200 },
      })
      .then((res) => {
        if (cancelled) return;
        const entries: ChatEntry[] = res.data.map((e) => {
          const p = (e.payload ?? {}) as { text?: string; email?: string; role?: string };
          return {
            id: ++chatIdRef.current,
            by: {
              userId: e.userId ?? "",
              email: p.email ?? "?",
              role: p.role ?? "",
            },
            text: p.text ?? "",
            at: e.createdAt,
          };
        });
        setChat(entries);
      })
      .catch(() => {
        // history fetch failure is non-fatal
      });
    return () => {
      cancelled = true;
    };
  }, [session.id]);

  // Load initial snapshot (for sessions that aren't IN_PROGRESS we still want to show last code)
  useEffect(() => {
    let cancelled = false;
    setLoadingSnapshot(true);
    api
      .get<CodeSnapshot | "">(`/sessions/${session.id}/snapshots/latest`)
      .then((res) => {
        if (cancelled) return;
        const snap = res.data;
        if (snap && typeof snap === "object" && "code" in snap) {
          setCode(snap.code);
          if (snap.language) setLanguage(snap.language);
          if (snap.createdAt) setLastSavedAt(new Date(snap.createdAt));
        }
      })
      .catch(() => {
        // 204 No Content or error — keep empty buffer
      })
      .finally(() => {
        if (!cancelled) setLoadingSnapshot(false);
      });
    return () => {
      cancelled = true;
    };
  }, [session.id]);

  // WebSocket lifecycle — only when session is open AND we have a token
  useEffect(() => {
    if (!isOpenSession || !token) return;

    const url = buildCollabUrl(session.id, token);
    setConn("connecting");

    let ws: WebSocket;
    try {
      ws = new WebSocket(url);
    } catch {
      setConn("error");
      return;
    }
    wsRef.current = ws;

    ws.onopen = () => setConn("open");
    ws.onclose = () => {
      setConn("closed");
      setParticipants([]);
    };
    ws.onerror = () => setConn("error");

    ws.onmessage = (e) => {
      let msg: ServerMessage;
      try {
        msg = JSON.parse(e.data);
      } catch {
        return;
      }
      switch (msg.type) {
        case "init":
          remoteUpdateRef.current = true;
          setCode(msg.code ?? "");
          if (msg.language) setLanguage(msg.language);
          setParticipants(msg.participants ?? []);
          break;
        case "edit":
          if (msg.by.userId === user?.id) break;
          remoteUpdateRef.current = true;
          setCode(msg.code ?? "");
          if (msg.language) setLanguage(msg.language);
          break;
        case "chat":
          pushChat({ by: msg.by, text: msg.text, at: msg.at });
          break;
        case "participant-joined":
          setParticipants((prev) => {
            if (prev.some((p) => p.userId === msg.userId)) return prev;
            return [...prev, { userId: msg.userId, email: msg.email, role: msg.role }];
          });
          break;
        case "participant-left":
          setParticipants((prev) => prev.filter((p) => p.userId !== msg.userId));
          break;
        case "error":
          toast.error(`Collab: ${msg.message}`);
          break;
      }
    };

    return () => {
      ws.close();
      wsRef.current = null;
      if (debounceRef.current) {
        window.clearTimeout(debounceRef.current);
        debounceRef.current = null;
      }
    };
  }, [session.id, isOpenSession, token, user?.id, pushChat]);

  const broadcastEdit = useCallback((nextCode: string, nextLang: string) => {
    const ws = wsRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) return;
    ws.send(JSON.stringify({ type: "edit", code: nextCode, language: nextLang }));
  }, []);

  const handleEditorChange = useCallback(
    (value: string | undefined) => {
      const next = value ?? "";
      setCode(next);
      if (remoteUpdateRef.current) {
        remoteUpdateRef.current = false;
        return;
      }
      dirtyRef.current = true;
      if (debounceRef.current) window.clearTimeout(debounceRef.current);
      debounceRef.current = window.setTimeout(() => {
        broadcastEdit(next, languageRef.current);
      }, EDIT_DEBOUNCE_MS);
    },
    [broadcastEdit]
  );

  const saveSnapshot = useCallback(
    async (snapshotType: "AUTO" | "MANUAL" | "FINAL" = "AUTO") => {
      if (snapshotType === "AUTO" && !canEdit) return;
      if (snapshotType === "AUTO" && !dirtyRef.current) return;
      const body: CodeSnapshotRequest = {
        language: languageRef.current,
        code: codeRef.current,
        snapshotType,
      };
      try {
        setSavingSnapshot(true);
        await api.post<CodeSnapshot>(`/sessions/${session.id}/snapshots`, body);
        dirtyRef.current = false;
        setLastSavedAt(new Date());
        if (snapshotType === "MANUAL") toast.success("Снимок сохранён");
      } catch (err) {
        const msg =
          isAxiosError<ApiError>(err) && err.response?.data?.message
            ? err.response.data.message
            : "Не удалось сохранить снимок";
        if (snapshotType !== "FINAL") toast.error(msg);
      } finally {
        setSavingSnapshot(false);
      }
    },
    [canEdit, session.id]
  );

  useImperativeHandle(
    ref,
    () => ({
      flushSnapshot: (type = "AUTO") => saveSnapshot(type),
    }),
    [saveSnapshot]
  );

  // Periodic auto-save when editing is allowed
  useEffect(() => {
    if (!canEdit) return;
    const timer = window.setInterval(() => {
      saveSnapshot("AUTO");
    }, AUTOSAVE_INTERVAL_MS);
    return () => window.clearInterval(timer);
  }, [canEdit, saveSnapshot]);

  const sendChat = useCallback(() => {
    const text = chatInput.trim();
    if (!text) return;
    const ws = wsRef.current;
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      toast.error("Нет соединения с чатом");
      return;
    }
    ws.send(JSON.stringify({ type: "chat", text }));
    setChatInput("");
  }, [chatInput]);

  const readOnly = !canEdit;

  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_320px]">
      <Card className="overflow-hidden">
        <CardHeader className="flex-row items-center justify-between space-y-0 gap-3 py-3">
          <div className="flex items-center gap-2">
            <CardTitle className="text-base">Редактор кода</CardTitle>
            <Badge variant="secondary" className="font-mono">{language}</Badge>
            <ConnBadge state={conn} disabled={!isOpenSession} />
          </div>
          <div className="flex items-center gap-2">
            {lastSavedAt && (
              <span className="text-xs text-muted-foreground">
                Сохранено {lastSavedAt.toLocaleTimeString("ru-RU")}
              </span>
            )}
            {canEdit && (
              <Button
                size="sm"
                variant="outline"
                onClick={() => saveSnapshot("MANUAL")}
                disabled={savingSnapshot}
              >
                {savingSnapshot ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
                Сохранить
              </Button>
            )}
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {loadingSnapshot ? (
            <div className="flex h-[500px] items-center justify-center text-muted-foreground">
              <Loader2 className="h-5 w-5 animate-spin" />
            </div>
          ) : (
            <Editor
              height="540px"
              language={monacoLanguage(language)}
              value={code}
              onChange={handleEditorChange}
              theme="vs-dark"
              options={{
                readOnly,
                minimap: { enabled: false },
                fontSize: 14,
                automaticLayout: true,
                tabSize: 2,
                scrollBeyondLastLine: false,
                wordWrap: "on",
              }}
            />
          )}
          {!canEdit && isOpenSession && (
            <div className="border-t bg-muted/50 px-4 py-2 text-xs text-muted-foreground">
              {session.status === "SCHEDULED"
                ? "Сессия ещё не начата — редактор откроется после старта."
                : "Просмотр в режиме только-чтение."}
            </div>
          )}
        </CardContent>
      </Card>

      <div className="space-y-4">
        <Card>
          <CardHeader className="py-3">
            <CardTitle className="flex items-center gap-2 text-sm">
              <Users className="h-4 w-4" /> Участники онлайн
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            {participants.length === 0 ? (
              <p className="text-xs text-muted-foreground">Пока никого нет.</p>
            ) : (
              participants.map((p) => (
                <div key={p.userId} className="flex items-center justify-between text-sm">
                  <span className="truncate">{p.email}</span>
                  <Badge variant="outline" className="font-normal text-xs">
                    {ROLE_LABELS[p.role as keyof typeof ROLE_LABELS] ?? p.role}
                  </Badge>
                </div>
              ))
            )}
          </CardContent>
        </Card>

        <Card className="flex flex-col">
          <CardHeader className="py-3">
            <CardTitle className="text-sm">Чат сессии</CardTitle>
          </CardHeader>
          <CardContent className="flex-1 space-y-2 max-h-[280px] overflow-y-auto">
            {chat.length === 0 ? (
              <p className="text-xs text-muted-foreground">Сообщений пока нет.</p>
            ) : (
              chat.map((m) => (
                <ChatItem key={m.id} entry={m} mine={m.by.userId === user?.id} />
              ))
            )}
          </CardContent>
          {isOpenSession && (
            <div className="flex gap-2 border-t p-3">
              <Input
                placeholder="Написать..."
                value={chatInput}
                onChange={(e) => setChatInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === "Enter" && !e.shiftKey) {
                    e.preventDefault();
                    sendChat();
                  }
                }}
                disabled={conn !== "open"}
              />
              <Button
                size="icon"
                onClick={sendChat}
                disabled={conn !== "open" || !chatInput.trim()}
              >
                <Send className="h-4 w-4" />
              </Button>
            </div>
          )}
        </Card>
      </div>
    </div>
  );
});

function ConnBadge({ state, disabled }: { state: ConnState; disabled: boolean }) {
  if (disabled) {
    return (
      <Badge variant="outline" className="gap-1 font-normal">
        <WifiOff className="h-3 w-3" /> офлайн
      </Badge>
    );
  }
  if (state === "open") {
    return (
      <Badge variant="default" className="gap-1 font-normal">
        <Wifi className="h-3 w-3" /> онлайн
      </Badge>
    );
  }
  if (state === "connecting") {
    return (
      <Badge variant="secondary" className="gap-1 font-normal">
        <Loader2 className="h-3 w-3 animate-spin" /> подключение
      </Badge>
    );
  }
  return (
    <Badge variant="destructive" className="gap-1 font-normal">
      <WifiOff className="h-3 w-3" /> не подключено
    </Badge>
  );
}

function ChatItem({ entry, mine }: { entry: ChatEntry; mine: boolean }) {
  return (
    <div className={`flex flex-col gap-0.5 ${mine ? "items-end" : "items-start"}`}>
      <div className="flex items-baseline gap-2">
        <span className="text-xs text-muted-foreground">{entry.by.email}</span>
        <span className="text-[10px] text-muted-foreground">
          {new Date(entry.at).toLocaleTimeString("ru-RU")}
        </span>
      </div>
      <div
        className={`rounded-md px-3 py-1.5 text-sm max-w-[90%] break-words ${
          mine ? "bg-primary text-primary-foreground" : "bg-secondary"
        }`}
      >
        {entry.text}
      </div>
    </div>
  );
}
