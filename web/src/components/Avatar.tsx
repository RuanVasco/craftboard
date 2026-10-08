import { User } from 'lucide-react';
import { useState } from 'react';

// One fixed size per name keeps a single cached image no matter how big each spot draws it.
const SOURCE_SIZE = 64;

// Offline mode has no skin server of its own, so heads come from the matching premium name, if any.
export function Avatar({ name, size = 28 }: { name: string; size?: number }) {
  const [failed, setFailed] = useState(false);
  if (failed) {
    return (
      <User
        className="shrink-0 rounded bg-primary-soft p-1 text-ink-muted"
        style={{ width: size, height: size }}
        aria-hidden
      />
    );
  }
  return (
    <img
      src={`https://mc-heads.net/avatar/${encodeURIComponent(name)}/${SOURCE_SIZE}`}
      alt=""
      width={size}
      height={size}
      className="shrink-0 rounded [image-rendering:pixelated]"
      onError={() => setFailed(true)}
    />
  );
}
