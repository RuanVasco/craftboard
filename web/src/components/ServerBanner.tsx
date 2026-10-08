import { Check, Copy, Radio, Server, Users } from 'lucide-react';
import { useState } from 'react';
import type { ServerInfo } from '../api';

interface Props {
  server: ServerInfo | null;
  live: boolean;
}

export function ServerBanner({ server, live }: Props) {
  const [copied, setCopied] = useState(false);

  const copyAddress = async () => {
    if (!server) return;
    try {
      await navigator.clipboard.writeText(server.address);
      setCopied(true);
      setTimeout(() => setCopied(false), 1500);
    } catch {
      // Clipboard needs https or localhost; the address is still visible to copy by hand.
    }
  };

  return (
    <header className="bg-banner text-banner-ink">
      <div className="mx-auto flex max-w-5xl flex-wrap items-center justify-between gap-4 px-4 py-5">
        <div className="flex min-w-0 items-center gap-3">
          <Server className="size-6 shrink-0 opacity-80" aria-hidden />
          <div className="min-w-0">
            <p className="truncate text-lg font-semibold">{server?.motd ?? 'Carregando...'}</p>
            <ServerStatus server={server} />
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={copyAddress}
            disabled={!server}
            className="flex items-center gap-2 rounded-lg bg-white/10 px-3 py-2 font-mono text-sm transition hover:bg-white/20"
            aria-label="Copiar endereço do servidor"
          >
            <span>{server?.address ?? '...'}</span>
            {copied ? <Check className="size-4" aria-hidden /> : <Copy className="size-4" aria-hidden />}
          </button>
          <span
            className="flex items-center gap-1.5 text-xs opacity-80"
            title={live ? 'Atualizando em tempo real' : 'Conectando ao servidor'}
          >
            <Radio className={`size-4 ${live ? '' : 'opacity-40'}`} aria-hidden />
            {live ? 'ao vivo' : 'conectando'}
          </span>
        </div>
      </div>
    </header>
  );
}

function ServerStatus({ server }: { server: ServerInfo | null }) {
  if (!server || server.online === null) {
    return <p className="text-sm opacity-70">Minecraft vanilla</p>;
  }
  if (!server.online) {
    return <p className="text-sm opacity-70">Servidor offline</p>;
  }
  const players = server.onlinePlayers ?? [];
  return (
    <p className="flex items-center gap-1.5 text-sm opacity-90" title={players.join(', ')}>
      <Users className="size-4" aria-hidden />
      {players.length} de {server.maxPlayers} online
    </p>
  );
}
