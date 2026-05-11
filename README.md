# Payroll Engine Demo

A finance-grade REST API that calculates the net salary from gross salary for German employees, applying the official BMF Programmablaufplan (PAP) for income tax and real social insurance rates.

## What This Is

A calculation engine that, given a gross monthly salary and a set of employee parameters, returns:

- The **exact net salary** after all deductions
- A **full deduction breakdown** (7 items: pension, health, care, unemployment insurance, income tax, solidarity surcharge, church tax)
- An **auditable calculation trace** with legal references for every step
- The **effective withholding rate**

The engine uses real German tax and social insurance rates, versioned by effective date, and labels its output as `OFFICIAL_PAP_LZZ2`.

## What This Is NOT

This is **not** a complete payroll system. It does not:

- Process payslips, manage employees, or persist data
- Implement the full PAP (only the LZZ=2 monthly salary path)
- Handle sonstige Bezuege (bonuses), Versorgungsbezuege, or Altersentlastungsbetrag
- Cover Mini-jobs (below Geringfuegigkeitsgrenze)
- Integrate with real ELStAM or DaBPV systems
- Replace professional payroll software (DATEV, SAP HR, etc.)

See [Scope and Limitations](#scope-and-limitations) for the full list.

## Stack

| Component | Version |
|-----------|---------|
| Kotlin | 2.3.20 |
| Spring Boot | 4.0.5 |
| Gradle | 9.2 |
| JDK | 25 |
| springdoc-openapi | 3.0.2 |
| Docker | Debian-based eclipse-temurin (glibc) |

## Quick Start

### Run locally

```bash
./gradlew bootRun
```

The API starts on `http://localhost:8080`.

### Run with Docker

```bash
docker build -t payroll-engine .
docker run -p 8080:8080 payroll-engine
```

### Run tests

```bash
./gradlew test
```

## API

### POST /api/v1/payroll/calculate

Calculate net salary from gross.

**Request:**

```json
{
  "grossMonthlySalary": 4500.00,
  "stkl": 1,
  "af": false,
  "zkf": 0.0,
  "lzzfreib": 0,
  "lzzhinzu": 0,
  "r": 1,
  "kvz": 2.50,
  "krv": 0,
  "pkv": 0,
  "pkpv": 0,
  "pvs": false,
  "pvz": true,
  "federalState": "BAYERN",
  "numberOfChildrenUnder25": 0,
  "birthDate": "1990-05-15",
  "calculationDate": "2025-06-15"
}
```

**Key parameters (PAP-isomorphic):**

| Parameter | PAP Variable | Description |
|-----------|-------------|-------------|
| `stkl` | STKL | Steuerklasse (tax class) 1-6 |
| `af` | AF | Faktorverfahren active (only STKL IV) |
| `f` | F | Factor, 3 decimals, (0.000, 1.000] |
| `zkf` | ZKF | Kinderfreibetraege (0.5 increments) |
| `lzzfreib` | LZZFREIB | Freibetrag for the pay period (cents) |
| `lzzhinzu` | LZZHINZU | Hinzurechnungsbetrag (cents) |
| `r` | R | Religion: 0 = no church tax, 1 = liable |
| `kvz` | KVZ | Zusatzbeitrag in full % (e.g., 2.50) |
| `krv` | KRV | 0 = RV-pflichtig, 1 = befreit, 2 = not in RV |
| `pkv` | PKV | 0 = GKV, 1 = PKV without AG subsidy, 2 = with |
| `pkpv` | PKPV | Monthly PKV contribution (cents) |
| `pvs` | PVS | true = Sachsen (PV special split) |
| `pvz` | PVZ | true = childless and >= 23 years old |

**Additional parameters (social insurance):**

| Parameter | Description |
|-----------|-------------|
| `federalState` | Bundesland (determines Kirchensteuer 8%/9%) |
| `numberOfChildrenUnder25` | For PV child-based rate reduction |
| `birthDate` | For PVZ age verification |
| `calculationDate` | Determines which PAP version and rates apply |

**Response includes:**

- `deductions[]`: Each with type, germanName, calculationBase, rate, amount, legalReference
- `netSalary`: The calculated net
- `effectiveWithholdingRate`: Total deductions / gross
- `calculationMode`: `"OFFICIAL_PAP_LZZ2"`
- `trace`: Full audit trail with traceId, papVersion, engineVersion, bpiSV, isUebergangsbereich, capsApplied, steps with legal references

### GET /api/v1/rates?date=2025-06-15

Returns the effective rates for a given date.

### GET /api/v1/rates

Returns all available rate periods.

### Observability

- `GET /actuator/health` - Health check
- `GET /actuator/prometheus` - Prometheus metrics
- `GET /swagger-ui.html` - OpenAPI documentation

## Architecture

```
com.marcoacuna.payroll
├── domain/                    # Pure calculation logic (NO framework imports)
│   ├── model/                 # Value objects: GrossSalary, TaxClass, FederalState, etc.
│   ├── rates/                 # Rate structures versioned by effective date
│   ├── audit/                 # CalculationTrace, CalculationStep
│   ├── calculation/           # Calculators: SV, PAP, KiSt, Uebergangsbereich
│   ├── service/               # PayrollCalculationService (orchestrator)
│   └── error/                 # PayrollError sealed class
├── application/               # Use cases and input ports
│   ├── usecase/               # CalculateNetSalaryUseCase, QueryRatesUseCase
│   └── port/input/            # Commands and queries
└── infrastructure/            # Framework-dependent code
    ├── adapter/input/rest/    # Controllers, DTOs, exception handler
    ├── adapter/output/        # InMemoryRatesAdapter (real German rates)
    ├── config/                # Spring beans, OpenAPI config
    ├── logging/               # Correlation ID filter
    └── metrics/               # Micrometer counters/timers
```

The domain layer has **zero** Spring imports. All calculation logic is testable without framework context.

## Calculation Pipeline

The order matters because of dependencies between deductions:

1. **Input validation** - Pre-PAP plausibility checks (AF only with STKL IV, ZKF only I-IV, etc.)
2. **Rate lookup** - Find rates effective on the calculation date
3. **Uebergangsbereich** - If 556 < gross <= 2000, compute reduced beitragspflichtige Einnahme (BE) per §20 Abs. 2a SGB IV
4. **Social insurance** - RV, KV, PV, AV on BE (or gross if regular)
5. **PAP** - Lohnsteuer + Soli + KiSt basis, using SV results for Vorsorgepauschale
6. **Kirchensteuer** - On BK from PAP, at state rate (8% or 9%)

## Scope and Limitations

### What the PAP implementation covers

- Monthly standard salary calculation (LZZ=2)
- All 6 Steuerklassen including:
  - STKL III (doubled Grundfreibetrag)
  - STKL IV with Faktorverfahren (AF/F)
  - STKL V/VI (MST5_6 special table)
- Vorsorgepauschale (UPEVS) with RV, KV/PV components
- §32a EStG progressive tariff with quadratic zones
- Solidaritaetszuschlag with exemption threshold and mitigation zone
- Kinderfreibetraege for KiSt/Soli basis (ZKF)
- LZZFREIB and LZZHINZU

### What it does NOT cover

| Feature | Why excluded |
|---------|-------------|
| Sonstige Bezuege (SONSTB) | Different PAP path, requires JFREIB/JHINZU |
| Versorgungsbezuege (VBEZ) | Separate deduction logic with VJAHR |
| Altersentlastungsbetrag (ALTER1/AJAHR) | Requires birth year before 1955 logic |
| VMT/VKAPA | Multi-year compensation rules |
| Mini-jobs (< Geringfuegigkeitsgrenze) | Separate contribution regime |
| Multiple employers | Requires STKL VI coordination |
| Real ELStAM integration | This is a calculation engine, not an employer system |

### Rounding

Inside the PAP engine, rounding follows the official specification per field. At the API boundary, monetary amounts are rounded to 2 decimal places (HALF_UP). Intermediate PAP fields are **not** rounded by default, matching the BMF's instruction that "Dezimalstellen werden abgeschnitten" only where the PAP specifies it.

### Rate accuracy

Rates are hardcoded from official German sources (BMAS, GKV-Spitzenverband, Deutsche Rentenversicherung, BMF). They are versioned by effective date to support mid-year PAP revisions (e.g., PAP 2025 January vs March). The KVZ (Zusatzbeitrag) is employee-specific input, not a system-wide constant.

## Testing

### Test suite

171 automated tests across 3 layers:

| Layer | Tests | What it covers |
|-------|-------|----------------|
| Domain unit tests | 51 | Calculators (RV, KV, PV, AV, PAP, KiSt), Uebergangsbereich, input validation, service pipeline |
| Integration tests | 3 | REST controllers, correlation ID, error responses |
| Canonical dataset replay | 117 | Parameterized E2E replay of all QA cases against the live API |

The canonical dataset replay (`CanonicalDatasetReplayTest`) reads `qa/canonical_test_cases.json` and executes every case as a `@ParameterizedTest` against the Spring Boot API. Cases are classified by assertion depth:

| Test type | Cases | Assertion level |
|-----------|-------|-----------------|
| `exact_match` | 34 | All 7 deductions + totals verified against real system output |
| `sv_formula` | 20 | SV deductions verified (formula-derived), PAP skipped |
| `oracle_pending` | 37 | Structure-only; PAP values pending BMF calculator verification |
| `error_contract` | 18 | HTTP status, error code, error field |
| `trace_contract` | 8 | Trace structure: steps count, engineVersion, bpiSV |

### Canonical QA dataset

`qa/canonical_test_cases.json` is the **single source of truth** for all QA cases. It contains 117 test cases covering:

- All 6 Steuerklassen (I-VI) including Faktorverfahren
- Uebergangsbereich boundary cases (556.00, 556.01, 1200, 1999.99, 2000, 2001)
- BBG cap verification (BBG_RV, BBG_KV, BBG_AV)
- PV child-dependent rates (0-5 children, Sachsen)
- Kirchensteuer 8% (BY/BW) and 9% (rest)
- PKV vs GKV, KRV exemptions
- All 4 error codes with correct HTTP status
- PAP version discriminant cases: gross=8100 shows different RV/LSt between 2025 (BBG_RV=8050) and 2026 (BBG_RV=8150)

### Enriched calculation trace

Every API response includes an audit trace with:

| Field | Purpose |
|-------|---------|
| `traceId` | UUID per calculation |
| `papVersion` | PAP version used (e.g., `PAP2025_Maerz`) |
| `ratesEffectiveFrom` | Effective date of rate set |
| `engineVersion` | Engine version (`1.0.0`) |
| `bpiSV` | Beitragspflichtige Einnahme used for SV (reduced in Uebergangsbereich) |
| `isUebergangsbereich` | Whether Midijob zone was active |
| `capsApplied` | Which BBG caps were hit (e.g., `["BBG_RV=8050.00", "BBG_KV=5512.50"]`) |
| `steps[]` | 7 steps with calculationBase, appliedRate, amount, legalReference, roundingInfo |

## Verification

Results can be verified against the official BMF calculator:

**https://www.bmf-steuerrechner.de/lst/**

The Lohnsteuer, Soli, and KiSt basis produced by this engine should match the BMF calculator for the same PAP version and input parameters. Social insurance contributions can be verified against the respective official sources.

## Legal References

| Deduction | Law |
|-----------|-----|
| Lohnsteuer | §39b EStG (PAP), §32a EStG (tariff) |
| Solidaritaetszuschlag | §3, §4 SolZG |
| Kirchensteuer | KiStG (per Bundesland) |
| Rentenversicherung | §158 SGB VI |
| Krankenversicherung | §223 SGB V |
| Pflegeversicherung | §55 Abs. 3 SGB XI |
| Arbeitslosenversicherung | §341 SGB III |
| Uebergangsbereich | §20 Abs. 2a SGB IV |
| ELStAM | §39e EStG |

## License

This project is for demonstration and educational purposes.
