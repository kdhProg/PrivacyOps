# PrivacyOps

> **Discover → Trace → Govern → Evidence**

PrivacyOps is an open-source **Privacy Engineering & Governance Toolkit** for analyzing how personal data is discovered, propagated, exposed, and governed inside application systems.

Instead of only asking:

> **"Does this system contain personal data?"**

PrivacyOps asks:

> **"Where was personal data discovered, where does it flow, how is it exposed, what governance controls apply to it, and what evidence supports the result?"**

PrivacyOps analyzes application source code, database metadata, MyBatis mappings, API endpoints, privacy policies, and governance controls to build an explainable view of personal-data handling.

The current prototype focuses on **Java / Spring / MyBatis / JDBC-based applications**.

---

## Why PrivacyOps?

Privacy-related checks are often fragmented across different layers.

A developer may know that a DTO contains an email address.

A DBA may know that a database table contains resident identifiers.

A security engineer may know that an API requires authorization.

A privacy officer may know that the table requires a retention and disposal policy.

But these pieces of information are rarely connected automatically.

PrivacyOps attempts to connect them into a single analysis pipeline.

```text
Database
   ↓
Mapper / Query
   ↓
Java DTO / Field
   ↓
API Endpoint
   ↓
Governance Controls
   ↓
Finding + Evidence
```

This allows PrivacyOps to reason about personal data not merely as isolated keywords, but as part of an **application data flow and governance context**.

---

# Core Concept

PrivacyOps is organized around four ideas.

| Concept | Description |
|---|---|
| **Discover** | Detect personal-data candidates from source code, database metadata, annotations, and configurable profiles |
| **Trace** | Connect data movement across Database → Mapper → DTO → API |
| **Govern** | Evaluate privacy-related controls such as access control, audit, retention, disposal, and API exposure |
| **Evidence** | Provide the source locations and reasoning behind each finding |

---

# Architecture

<img width="1189" height="731" alt="Image" src="https://github.com/user-attachments/assets/3cb861a1-d324-4ce7-9956-a04855d20eae" />

PrivacyOps separates **collection**, **classification**, **flow analysis**, **governance evaluation**, and **reporting** into independent layers.

This architecture is designed to allow additional frameworks, governance rules, classifiers, reporting formats, and enterprise systems to be integrated without replacing the core analysis model.

---

# Key Features

## 1. Personal Data Discovery

PrivacyOps detects privacy-related fields using multiple strategies.

### Built-in Name Pattern Detection

Common field names can be automatically classified.

Examples:

```text
name
emailAddress
phoneNumber
residentNumber
```

Possible classifications include:

```text
NAME
EMAIL
PHONE_NUMBER
NATIONAL_IDENTIFIER
```

---

## 2. Custom Privacy Annotation

Applications can explicitly identify privacy-sensitive fields when automatic detection is insufficient.

Example:

```java
@PrivacyData
private String residentNumber;
```

Explicit annotations can complement heuristic classification and make privacy intent visible directly in application code.

---

## 3. Custom Risk Profile

Privacy sensitivity differs between organizations and systems.

PrivacyOps therefore supports configurable risk profiles.

Example:

```yaml
keywords:

  internal_token:
    type: NATIONAL_IDENTIFIER
    weight: 10

  employee_no:
    type: NATIONAL_IDENTIFIER
    weight: 8
```

A field that is not recognized by the default classifier can therefore become a privacy candidate through organization-specific configuration.

For example:

```java
private String internalToken;
```

can be detected through a custom Risk Profile without modifying PrivacyOps source code.

This makes privacy classification **policy-driven rather than completely hard-coded**.

---

## 4. MyBatis Query Analysis

PrivacyOps analyzes MyBatis mapper definitions and extracts information such as:

- Mapper namespace
- Query ID
- Result type
- Referenced tables
- Selected columns

Example:

```xml
<select id="selectMember"
        resultType="com.example.legacy.dto.MemberDto">

    SELECT
        MEMBER_ID,
        NAME,
        RESIDENT_NUMBER,
        EMAIL_ADDRESS,
        PHONE_NUMBER
    FROM TB_MEMBER

</select>
```

The extracted information becomes part of the internal Fact Model.

---

## 5. Personal Data Flow Tracing

PrivacyOps connects privacy-related information across application layers.

Example:

```text
TB_MEMBER.RESIDENT_NUMBER
        ↓
MemberMapper.selectMember
        ↓
MemberDto.residentNumber
        ↓
GET /members/{id}
```

The analysis is represented internally through relationships such as:

```text
MAPPER_RESULT
API_RESPONSE
DATABASE_MAPPER
```

This provides the foundation for reasoning about **where personal data originates and where it may be exposed**.

---

## 6. API Privacy Exposure Detection

PrivacyOps analyzes Spring MVC endpoints and connects response objects with detected privacy fields.

Example:

```text
GET /members/{id}
  name           → NAME
  residentNumber → NATIONAL_IDENTIFIER
  emailAddress   → EMAIL
  phoneNumber    → PHONE_NUMBER
```

Potential privacy exposure is then reported as a Finding.

Example:

```text
[HIGH] PRIV-API-001

Personal data in API response

Privacy field MemberDto.emailAddress (EMAIL)
may be exposed through GET /members/{id}.
```

---

# Privacy Governance Analysis

Finding personal data is only the first step.

PrivacyOps also evaluates whether privacy-related governance controls are represented in the analyzed system.

The current prototype includes checks for:

| Rule | Purpose |
|---|---|
| `PRIV-API-001` | Personal data exposure through API responses |
| `PRIV-ACCESS-001` | Missing recognized access control |
| `PRIV-AUDIT-001` | Missing recognized audit control |
| `PRIV-POLICY-001` | Missing privacy resource policy |
| `PRIV-RETENTION-001` | Missing retention policy |
| `PRIV-DISPOSAL-001` | Missing disposal policy |

Example:

```text
[CRITICAL] PRIV-RETENTION-001

Missing retention policy

Privacy resource TB_MEMBER has no retention policy.
```

---

# Policy as Code

Privacy governance requirements can be described outside application source code.

Example:

```yaml
resources:

  TB_MEMBER:
    purpose: member-management
    retention: 3y
    disposal: scheduled-delete
```

PrivacyOps can compare discovered resources against declared governance policies.

For example, an incomplete policy:

```yaml
resources:

  TB_MEMBER:
    purpose: member-management
```

can produce:

```text
Missing retention policy
Missing disposal policy
```

This enables privacy requirements to be treated as **version-controlled configuration**.

---

# Configurable Rule Packs

Different organizations have different privacy requirements and risk tolerances.

PrivacyOps supports external Rule Packs.

Example:

```yaml
name: strict-privacy-example

rules:

  PRIV-ACCESS-001:
    enabled: true
    severity: CRITICAL

  PRIV-AUDIT-001:
    enabled: true
    severity: CRITICAL

  PRIV-RETENTION-001:
    enabled: true
    severity: CRITICAL

  PRIV-DISPOSAL-001:
    enabled: true
    severity: CRITICAL

  PRIV-API-001:
    enabled: true
    severity: HIGH
```

This allows organizations to adjust governance behavior without modifying the PrivacyOps engine.

---

# Risk-Based Privacy Analysis

PrivacyOps can assign different weights to different privacy types.

Example:

```text
NATIONAL_IDENTIFIER    weight=10    CRITICAL
EMAIL                  weight=3     MEDIUM
PHONE_NUMBER           weight=3     MEDIUM
NAME                   weight=1     LOW
```

This prevents every personal-data field from being treated as equally sensitive.

Risk Profiles can further customize these values for organization-specific data.

---

# Evidence-Based Findings

PrivacyOps findings are designed to explain **why a result was generated**.

Example:

```text
[CRITICAL] PRIV-ACCESS-001
Missing access control

Privacy API GET /members/{id}
has no recognized access control.

Evidence:

[DATA_FLOW]
Privacy API detected
GET /members/{id}

[SOURCE_CODE]
Spring Security access control
No recognized Spring Security access control was found.
```

Evidence can include:

```text
SOURCE_CODE
DATA_FLOW
POLICY
```

along with source file locations.

The goal is to make findings **traceable and reviewable**, rather than returning only a risk score.

---

# Governance Coverage

PrivacyOps summarizes governance coverage across multiple privacy-control dimensions.

Example:

```text
Resource Policy   ✓
Retention         ✗
Disposal          ✗
Access Control    ✗
Audit Control     ✗
```

The result can also be represented as an overall Governance Coverage score.

Example:

```text
Governance Coverage: 20%
```

This provides a compact view of privacy-governance readiness while preserving detailed evidence underneath.

---

# Extensible Integration through SPI

PrivacyOps is designed as an extensible toolkit rather than a closed privacy scanner.

External systems can participate in privacy analysis through provider interfaces.

One example is access-control detection.

```text
PrivacyOps
     │
     ├── Spring Security Provider
     │
     ├── External IAM Provider
     │
     ├── SSO Provider
     │
     └── Custom Enterprise Provider
```

The current project includes an example IAM plugin demonstrating this extension mechanism.

When an external provider is available on the classpath, PrivacyOps can discover it and use it during governance analysis.

Example:

```text
Access Control Providers
--------------------------------
- spring-security
- example-iam
```

This architecture is intended to support future integration with:

- IAM systems
- SSO platforms
- HR / organizational systems
- Audit platforms
- SIEM systems
- Database security systems
- Custom enterprise governance systems

without tightly coupling these systems to the PrivacyOps core.

---

# Output Formats

PrivacyOps currently provides multiple forms of output.

## CLI

Designed for developers, local analysis, CI pipelines, and demonstrations.

```text
PrivacyOps 0.1.0

Scan Summary
--------------------------------
Facts                  : 18
Privacy Candidates     : 6
Warnings               : 0
```

---

## HTML Report

A human-readable report provides a visual summary of:

- detected privacy data
- privacy types
- governance coverage
- highest privacy risk
- data flows
- findings
- evidence
- policy status

---

## JSON Report

PrivacyOps can also generate machine-readable results.

Example structure:

```json
{
  "generator": "PrivacyOps",
  "version": "0.1.0",
  "summary": {
    "facts": 18,
    "privacyCandidates": 6,
    "dataFlows": 16,
    "findings": 9,
    "criticalFindings": 4,
    "governanceCoverage": 20,
    "highestPrivacyRisk": "CRITICAL"
  }
}
```

The JSON format allows PrivacyOps results to be consumed by other tools and automation pipelines.

Potential integrations include:

```text
CI/CD
Dashboards
Security platforms
Governance systems
AI-assisted analysis
Custom reporting
```

---

# Example Analysis Scenario

The repository contains a sample legacy member-management system.

```text
samples/
└── legacy-member-system/
```

The sample intentionally contains privacy-governance issues.

PrivacyOps can discover:

```text
NAME
EMAIL
PHONE_NUMBER
NATIONAL_IDENTIFIER
```

and reconstruct flows such as:

```text
TB_MEMBER.RESIDENT_NUMBER
        ↓
MemberMapper
        ↓
MemberDto.residentNumber
        ↓
GET /members/{id}
```

An incomplete privacy policy can then trigger governance findings such as:

```text
Missing access control
Missing audit control
Missing retention policy
Missing disposal policy
```

After the corresponding governance information is supplied, these findings can be reduced or eliminated.

This sample is intended to demonstrate the difference between:

> **Personal data exists**

and

> **Personal data exists, flows through specific components, reaches a specific API, and is subject to specific governance controls.**

---

# Quick Start

## Requirements

```text
Java 17+
Apache Maven
```

Check your environment:

```bash
java -version
mvn -version
```

---

## Build

Clone the repository and build all modules:

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd PrivacyOps

mvn clean verify
```

---

# Running PrivacyOps

Basic scan:

```bash
java -jar privacyops-cli/target/privacyops-cli-0.1.0.jar scan ./samples/legacy-member-system
```

Scan with a privacy policy:

```bash
java -jar privacyops-cli/target/privacyops-cli-0.1.0.jar scan \
  ./samples/legacy-member-system \
  --policy ./samples/legacy-member-system/policies/incomplete.yml
```

Run a full analysis with custom configuration:

```bash
java -jar privacyops-cli/target/privacyops-cli-0.1.0.jar scan \
  ./samples/legacy-member-system \
  --policy ./samples/legacy-member-system/policies/incomplete.yml \
  --risk-profile ./samples/legacy-member-system/risk-profile.yml \
  --rule-pack ./rulepacks/strict-example.yml \
  --report ./privacyops-report.html \
  --json-report ./privacyops-report.json
```

> Depending on the packaging configuration, the executable command may differ.  
> See the generated artifacts under `privacyops-cli/target/` after building the project.

---

# Project Structure

```text
PrivacyOps
│
├── privacyops-domain
│   └── Core domain model
│       Facts / Findings / Evidence / Policies / Data Flows
│
├── privacyops-analyzer
│   └── Static analysis and governance engine
│       Java / Spring / MyBatis / JDBC analysis
│
├── privacyops-cli
│   └── Command-line interface
│
├── privacyops-report
│   └── HTML / JSON reporting
│
├── privacyops-example-iam-plugin
│   └── Example SPI integration
│
├── rulepacks
│   └── Example governance Rule Packs
│
├── samples
│   └── Demonstration applications and policies
│
└── docs
    └── Documentation and architecture resources
```

---

# Analysis Pipeline

Internally, PrivacyOps follows a staged analysis model.

```text
              ┌───────────────┐
              │    Scanner    │
              └───────┬───────┘
                      ↓
              ┌───────────────┐
              │  Fact Model   │
              └───────┬───────┘
                      ↓
              ┌───────────────┐
              │  Classifier   │
              └───────┬───────┘
                      ↓
              ┌───────────────┐
              │Data Flow Linker│
              └───────┬───────┘
                      ↓
              ┌───────────────┐
              │Governance Rule│
              │    Engine     │
              └───────┬───────┘
                      ↓
              ┌───────────────┐
              │Finding/Evidence│
              └───────┬───────┘
                      ↓
             CLI / HTML / JSON
```

---

# Design Principles

PrivacyOps is being developed around several principles.

### Explainability over black-box scoring

A finding should include evidence explaining why it exists.

### Configuration over hard-coding

Privacy requirements differ across organizations.

Policies, Risk Profiles, and Rule Packs therefore remain configurable.

### Integration over replacement

PrivacyOps is not intended to replace IAM, SIEM, audit, HR, or database-security systems.

Instead, it provides an analysis layer that can integrate signals from those systems.

### Data flow over isolated detection

Finding a privacy-related keyword is useful.

Understanding where that information flows is more useful.

### Open extension over vendor lock-in

SPI-based integration allows additional governance environments to participate without modifying the core engine.

---

# What PrivacyOps Is — and Is Not

PrivacyOps **is**:

- a privacy engineering toolkit
- a static analysis framework
- a privacy data-flow analyzer
- a policy and governance validation engine
- an evidence generator
- an extensible integration foundation

PrivacyOps is **not**:

- a DLP replacement
- an IAM product
- a SIEM product
- a database encryption product
- a legal compliance certification tool
- a guarantee of regulatory compliance

PrivacyOps provides technical evidence that can support privacy engineering and governance activities.

Final legal and organizational compliance decisions remain the responsibility of the organization using the tool.

---

# Current Scope

PrivacyOps `0.1.0` is an early-stage prototype.

The current implementation primarily focuses on:

```text
Java
Spring / Spring MVC
MyBatis
JDBC metadata
YAML-based privacy policies
YAML-based Risk Profiles
YAML-based Rule Packs
CLI / HTML / JSON reporting
SPI-based access-control integration
```

The project intentionally uses a modular architecture so that additional technologies can be introduced incrementally.

---

# Roadmap

Potential future development areas include:

- Additional ORM support
- JPA / Hibernate analysis
- Additional web frameworks
- Additional database metadata adapters
- More sophisticated inter-procedural data-flow analysis
- Authentication / authorization provider integrations
- Audit and SIEM provider SPI
- SARIF output
- CI/CD integration
- GitHub Actions integration
- IDE integration
- Additional privacy rule packs
- Organization-specific governance profiles
- International privacy-framework mappings
- Automated SBOM generation
- Privacy architecture visualization
- AI-assisted explanation of generated evidence

AI integration, where introduced, is intended to operate as an **optional interpretation or recommendation layer over structured PrivacyOps evidence**, rather than replacing the deterministic core analysis engine.

---

# Technology Stack

PrivacyOps is implemented primarily in Java.

Major technologies used by the project include:

| Technology | Purpose |
|---|---|
| Java 17 | Core implementation |
| Maven | Multi-module build and dependency management |
| JavaParser | Java source and AST analysis |
| Picocli | Command-line interface |
| Jackson | JSON/YAML processing |
| SnakeYAML | YAML processing |
| JUnit 5 | Automated testing |
| H2 | JDBC scanner testing |

---

# Open Source

PrivacyOps is designed as an open-source project.

The project aims to provide reusable building blocks for:

- developers
- security engineers
- privacy engineers
- system architects
- governance teams
- researchers
- open-source contributors

Rather than embedding organization-specific privacy logic directly into the engine, PrivacyOps attempts to expose reusable models and extension points that can be adapted to different environments.

---

# Contributing

Contributions are welcome.

Potential contribution areas include:

- privacy classifiers
- framework scanners
- database adapters
- Data Flow Linkers
- Privacy Rules
- Rule Packs
- Risk Profiles
- SPI providers
- reporters
- sample applications
- documentation

For significant changes, opening an issue before implementation is recommended so that the proposed design can be discussed.

---

# Security and Analysis Limitations

PrivacyOps performs static and metadata-based analysis.

Results may contain false positives or false negatives depending on:

- application architecture
- naming conventions
- runtime behavior
- reflection
- dynamically generated queries
- custom frameworks
- external services
- incomplete configuration

PrivacyOps findings should therefore be interpreted as **engineering evidence for review**, not as an automatic legal or compliance determination.

---

# License

Copyright 2026 PrivacyOps Contributors

Licensed under the **Apache License, Version 2.0**.

You may use, modify, and distribute this project in accordance with the terms of the license.

See the `LICENSE` file for details.

---

# Project Vision

PrivacyOps ultimately aims to provide a common technical layer between:

```text
Application Development
        +
Cybersecurity
        +
Privacy Engineering
        +
Data Governance
```

so that personal data can be:

> **Discovered, traced, governed, and explained.**

**PrivacyOps — Discover → Trace → Govern → Evidence**