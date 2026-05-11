# Evidencia de validacion — payroll-engine-demo v1.0.0

Motor: `OFFICIAL_PAP_LZZ2` | Engine: `1.0.0` | Fecha: 2026-04-01

## 1. Resumen ejecutivo

| Metrica | Valor |
|---------|-------|
| Tests automatizados totales | **171** |
| Tests fallidos | **0** |
| Dataset canonico (`qa/canonical_test_cases.json`) | **117 casos** |
| Casos con valores exactos verificados (`exact_match`) | 34 |
| Casos con SV verificada por formula (`sv_formula`) | 20 |
| Casos pendientes de oraculo BMF (`oracle_pending`) | 37 |
| Contrato de error verificado (`error_contract`) | 18 |
| Contrato de trace verificado (`trace_contract`) | 8 |

Todos los 117 casos se ejecutan automaticamente via `CanonicalDatasetReplayTest` (SpringBootTest parameterizado) como parte de `./gradlew test`.

## 2. Fuente unica de verdad

`qa/canonical_test_cases.json` es el unico artefacto de QA. No hay archivos derivados. El replay runner lee este JSON y ejecuta cada caso contra la API real.

Distribucion por categoria:

| Categoria | Casos | Tipo predominante |
|-----------|-------|-------------------|
| happy_path | 12 | exact_match / sv_formula |
| golden_master | 20 | oracle_pending (pendiente BMF) |
| validation | 15 | error_contract |
| uebergangsbereich | 10 | exact_match |
| social_insurance | 15 | sv_formula / exact_match |
| pap_tax | 10 | oracle_pending |
| kirchensteuer | 6 | exact_match / oracle_pending |
| versioning | 11 | exact_match |
| trace | 5 | trace_contract |
| api | 5 | trace_contract / error_contract |
| boundary | 8 | exact_match / sv_formula |

## 3. Valores verificados contra el sistema real

### 3.1 Happy path completos (exact_match)

```
A-001  gross=0.00     STKL I   → all 0, net=0.00
A-002  gross=4500     STKL I   → RV=418.50 KV=384.75 PV=108.00 AV=58.50 LSt=445.50 Soli=0 total=1415.25 net=3084.75
A-004  gross=5000     STKL III → RV=465.00 KV=407.50 PV=77.50  AV=65.00 LSt=36.50  Soli=0 total=1051.50 net=3948.50
A-006  gross=3000     STKL V   → RV=279.00 KV=256.50 PV=72.00  AV=39.00 LSt=243.58 Soli=0 total=890.08  net=2109.92
A-007  gross=2500     STKL VI  → RV=232.50 KV=213.75 PV=60.00  AV=32.50 LSt=260.75 Soli=0 total=799.50  net=1700.50
A-008  gross=8000     STKL I   → RV=744.00 KV=471.32 PV=132.30 AV=104.00 LSt=1626.91 Soli=13.81 total=3092.34 net=4907.66
B-012  gross=4500     STKL IV F=0.900 → LSt=400.91 KiSt=26.27 total=1369.93 net=3130.07
G-001  gross=4500     r=1 Bayern  → KiSt=35.64 (BK=445.50 x 0.08)
G-002  gross=4500     r=1 NRW     → KiSt=40.10 (BK=445.50 x 0.09)
```

### 3.2 Uebergangsbereich E2E (exact_match con trace enriquecido)

```
gross=556.00   → bpiSV=556.00   midijob=false  RV=51.71  KV=47.54  PV=13.34  AV=7.23   total=119.82  net=436.18
gross=556.01   → bpiSV=371.59   midijob=true   RV=34.56  KV=31.77  PV=8.92   AV=4.83   total=80.08   net=475.93
gross=1200.00  → bpiSV=1097.83  midijob=true   RV=102.10 KV=93.86  PV=26.35  AV=14.27  total=236.58  net=963.42
gross=1999.99  → bpiSV=1999.99  midijob=true   RV=186.00 KV=171.00 PV=48.00  AV=26.00  total=431.00  net=1568.99
gross=2000.00  → bpiSV=2000.00  midijob=false  RV=186.00 KV=171.00 PV=48.00  AV=26.00  total=431.00  net=1569.00
gross=2001.00  → bpiSV=2001.00  midijob=false  RV=186.09 KV=171.09 PV=48.02  AV=26.01  total=431.21  net=1569.79
```

### 3.3 Casos discriminantes entre versiones PAP (exact_match)

Demuestran que el motor produce resultados numericamente distintos cuando cambian las tasas/BBG entre periodos.

**gross=8100 (cruza cambio BBG_RV 8050 → 8450):**

| Campo | 2025 (PAP2025_Maerz) | 2026 (PAP2026) | Delta |
|-------|---------------------|----------------|-------|
| RV | 748.65 (capped BBG 8050) | 753.30 (not capped) | +4.65 |
| KV | 471.32 (capped BBG 5512.50) | 496.97 (capped BBG 5812.50) | +25.65 |
| PV | 132.30 (capped) | 139.50 (capped BBG 5812.50) | +7.20 |
| AV | 104.65 (capped BBG 8050) | 105.30 (not capped) | +0.65 |
| LSt | 1667.00 | 1665.00 | -2.00 |
| Soli | 18.58 | 18.34 | -0.24 |
| capsApplied | BBG_RV, BBG_KV, BBG_AV | BBG_KV | |
| **net** | **4957.50** | **4921.59** | **-35.91** |

**gross=5550 (cruza cambio BBG_KV 5512.50 → 5812.50):**

| Campo | 2025 (PAP2025_Maerz) | 2026 (PAP2026) | Delta |
|-------|---------------------|----------------|-------|
| KV | 471.32 (capped BBG 5512.50) | 474.53 (not capped) | +3.21 |
| PV | 132.30 (capped) | 133.20 (not capped) | +0.90 |
| capsApplied | BBG_KV | (ninguno) | |
| **net** | **3608.42** | **3604.31** | **-4.11** |

### 3.4 Seguridad social (exact_match / sv_formula)

Todas las formulas SV producen valores identicos al sistema:

| Formula | Verificado con |
|---------|---------------|
| RV: min(bpiSV, BBG_RV) x 0.093 | A-002, A-008, E-002, E-013 |
| KV: min(bpiSV, BBG_KV) x (0.073 + KVZ/200) | A-002, E-001, E-014 |
| PV kinderlos: base x 0.024 | A-002, E-004 |
| PV 2 hijos: base x 0.0155 | E-005 |
| PV 5 hijos: base x 0.008 | E-006 |
| PV Sachsen: base x (rate + 0.005) | E-003 |
| AV: min(bpiSV, BBG_AV) x 0.013 | A-002, E-002 |
| KRV=1: RV=0, AV activo | E-008 |
| KRV=2: RV=0, AV=0 | E-009 |
| PKV: KV=0 | E-007 |

### 3.5 Contrato de errores (error_contract)

El sistema tiene exactamente 5 codigos de error (4 de cliente, 1 de servidor):

| Codigo | HTTP | `field` | Casos verificados |
|--------|------|---------|-------------------|
| `INVALID_INPUT` | 400 | nombre del campo | C-001 a C-005, C-007 a C-013, C-015 |
| `INVALID_GROSS_SALARY` | 400 | null | C-009 |
| `INCOMPATIBLE_PARAMETERS` | 422 | null | C-006, C-014 |
| `UNSUPPORTED_PERIOD` | 404 | null | H-001 |
| `CALCULATION_OVERFLOW` | 500 | null | (no verificado — proteccion contra desborde aritmético) |

Nota: Jakarta Bean Validation errors (`MethodArgumentNotValidException`) se unifican bajo `INVALID_INPUT` con el `field` correspondiente.

### 3.6 Trace enriquecido

Cada response incluye:

```json
"trace": {
  "traceId": "a9d09b5e-799e-4efc-94be-a1e902dc13f0",
  "timestamp": "2026-04-01T13:28:35.295763Z",
  "papVersion": "PAP2025_Maerz",
  "ratesEffectiveFrom": "2025-03-01",
  "engineVersion": "1.0.0",
  "bpiSV": 4500.0,
  "uebergangsbereich": false,
  "capsApplied": [],
  "steps": [ ... 7 steps ... ]
}
```

Campos verificados:
- `engineVersion` presente en todas las responses
- `bpiSV` correcto para Uebergangsbereich (371.59 para gross=556.01) y regular (=gross)
- `uebergangsbereich` true/false correcto en 6 puntos de frontera
- `capsApplied` lista correcta de BBGs cuando aplican (verificado con gross=8100 y 5550)

## 4. Versionado de tasas

| Fecha calculo | papVersion | ratesEffectiveFrom | BBG_RV | BBG_KV |
|--------------|------------|-------------------|--------|--------|
| 2025-01-01 a 2025-02-28 | PAP2025_Januar | 2025-01-01 | 8050 | 5512.50 |
| 2025-03-01 a 2025-12-31 | PAP2025_Maerz | 2025-03-01 | 8050 | 5512.50 |
| 2026-01-01 en adelante | PAP2026 | 2026-01-01 | 8450 | 5812.50 |

Verificado en: H-002 (feb 2025 → Januar), H-003 (mar 2025 → Maerz), H-006 (2026 → PAP2026).

## 5. Pendiente

| Item | Estado | Accion |
|------|--------|--------|
| 37 casos `oracle_pending` | Valores PAP (LSt, Soli, BK) no verificados contra BMF | Completar contra bmf-steuerrechner.de/lst/ |
| Mutation testing | No ejecutado | Ejecutar sobre PapCalculator, CareInsuranceCalculator, UebergangsbereichCalculator |
| Property-based tests | No implementados | Invariantes: net=gross-total, deducciones >= 0, monotonia SV |
| Contract snapshots | No congelados | Snapshot JSON exito + snapshot JSON por cada error code |

## 6. Como reproducir

```bash
# Ejecutar todos los 171 tests (incluye replay de 117 casos canonicos)
./gradlew test

# Ejecutar solo el replay runner
./gradlew test --tests "*.CanonicalDatasetReplayTest"

# Ver reporte HTML
open build/reports/tests/test/index.html
```
