/** 将耗时毫秒格式化为秒：保留小数点后一位，四舍五入 */
export function formatDurationSeconds(durationMs?: number | null): string {
  if (durationMs == null || Number.isNaN(Number(durationMs))) return '-';
  const secRoundedTenth = Math.round(Number(durationMs) / 100) / 10;
  return `${secRoundedTenth.toFixed(1)} s`;
}
