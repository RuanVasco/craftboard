import { useEffect, useState } from 'react';
import { fetchRanking, type Period, type RankingPage, type RankingSort } from './api';

const PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;
// A save flushes every online player at once, so their events are folded into one request.
const RELOAD_DEBOUNCE_MS = 1000;

export function useRanking(period: Period, statsVersion: number) {
  const [sort, setSort] = useState<RankingSort>('playTime');
  const [query, setQuery] = useState('');
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [refresh, setRefresh] = useState(0);
  const [data, setData] = useState<RankingPage | null>(null);
  const [error, setError] = useState<string | null>(null);

  // Advancement counts are totals, so outside "all" the ranking falls back to play time.
  const effectiveSort = period !== 'all' && sort === 'advancements' ? 'playTime' : sort;

  // A different list starts from its first page.
  const [listKey, setListKey] = useState(`${period}|${effectiveSort}|${search}`);
  const currentKey = `${period}|${effectiveSort}|${search}`;
  if (listKey !== currentKey) {
    setListKey(currentKey);
    setPage(0);
  }

  useEffect(() => {
    const timer = setTimeout(() => setSearch(query.trim()), SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [query]);

  useEffect(() => {
    if (statsVersion === 0) return;
    const timer = setTimeout(() => setRefresh((r) => r + 1), RELOAD_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [statsVersion]);

  useEffect(() => {
    // Only the latest request may land, whatever order the responses arrive in.
    let active = true;
    fetchRanking({ period, sort: effectiveSort, q: search, page, size: PAGE_SIZE })
      .then((result) => {
        if (!active) return;
        setData(result);
        setError(null);
        // Players can drop out of a period between requests, leaving the page past the end.
        if (result.totalPages > 0 && page >= result.totalPages) setPage(result.totalPages - 1);
      })
      .catch((e: Error) => {
        if (active) setError(e.message);
      });
    return () => {
      active = false;
    };
  }, [period, effectiveSort, search, page, refresh]);

  return { data, error, sort: effectiveSort, setSort, query, setQuery, page, setPage };
}
