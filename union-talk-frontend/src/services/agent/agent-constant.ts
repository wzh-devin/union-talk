export const AGENT_RUN_PAGE_SIZE = 20
export const AGENT_TEXT_REVEAL_CHARACTER_COUNT_PER_SECOND = 500
export const AGENT_TEXT_REVEAL_INTERVAL_MILLISECONDS =
  1000 / AGENT_TEXT_REVEAL_CHARACTER_COUNT_PER_SECOND

export const DEEPSEEK_MODEL_OPTION_LIST = [
  {
    value: 'deepseek-v4-flash',
    label: 'deepseek-v4-flash',
  },
  {
    value: 'deepseek-v4-pro',
    label: 'deepseek-v4-pro',
  },
] as const
