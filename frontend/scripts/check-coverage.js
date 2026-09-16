const { spawnSync } = require('child_process');

const THRESHOLDS = {
  Statements: 60,
  Branches: 40,
  Functions: 42,
  Lines: 60
};

const result = spawnSync(
  'npx',
  ['ng', 'test', '--watch=false', '--browsers=ChromeHeadless', '--code-coverage'],
  { encoding: 'utf8', shell: process.platform === 'win32' }
);

process.stdout.write(result.stdout ?? '');
process.stderr.write(result.stderr ?? '');

if (result.status !== 0) {
  process.exit(result.status ?? 1);
}

const output = result.stdout ?? '';
const failures = [];

for (const [metric, minPercent] of Object.entries(THRESHOLDS)) {
  const match = output.match(new RegExp(`${metric}\\s*:\\s*([\\d.]+)%`));
  if (!match) {
    failures.push(`${metric}: nao encontrado no resumo de cobertura`);
    continue;
  }
  const actual = Number(match[1]);
  if (actual < minPercent) {
    failures.push(`${metric}: ${actual}% abaixo do minimo exigido (${minPercent}%)`);
  }
}

if (failures.length > 0) {
  console.error('\nCobertura de testes abaixo do minimo:');
  failures.forEach((failure) => console.error(`  - ${failure}`));
  process.exit(1);
}

console.log('\nCobertura de testes dentro dos limites minimos.');
