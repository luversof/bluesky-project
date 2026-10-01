// 게임 경로 → work/files 안 파일 이름(소문자, / → @) — export-skill-desc 와 parse-gems2 가 같은 규칙을 쓴다.
export const localName = (p) => p.toLowerCase().replace(/\//g, "@");
