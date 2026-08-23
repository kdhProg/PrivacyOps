# PrivacyOps

PrivacyOps is an open-source privacy governance analyzer
for software development environments.

It discovers personal-data related artifacts and connects:

`Database -> Mapper -> Java DTO -> API`

PrivacyOps then evaluates whether the discovered data flows
are accompanied by governance controls such as:

- access control
- audit logging intent
- retention policy
- disposal policy

## Why PrivacyOps?

Personal data itself is not necessarily a problem.

The real risk is personal data that exists,
moves, or remains in systems without clear ownership,
policy, or lifecycle controls.

PrivacyOps aims to make those invisible privacy-management gaps
visible to developers and privacy teams.

## Quick Start

Scan a project:

```bash
privacyops scan ./samples/legacy-member-system
```
Scan with a governance policy:
```bash
privacyops scan ./samples/legacy-member-system 
--policy ./samples/legacy-member-system/policies/incomplete.yml
```
Generate an HTML report:
```bash
privacyops scan ./samples/legacy-member-system 
  --policy ./samples/legacy-member-system/policies/incomplete.yml 
  --report ./privacyops-report.html
```


