# PrivacyOps Legacy Member System Demo

This sample represents a legacy Spring + MyBatis application
that handles personal information.

The scenario demonstrates how PrivacyOps detects privacy data flows
and evaluates whether management controls are attached to them.

## Scenario

The application reads personal data from `TB_MEMBER`
and returns it through:

`GET /members/{id}`

Detected flow:

`TB_MEMBER -> MyBatis -> MemberDto -> Spring API`

---

## 1. Unmanaged state

The default controller has:

- no recognized access control
- no privacy audit declaration

Use:

`policies/incomplete.yml`

The policy defines the processing purpose,
but intentionally omits retention and disposal controls.

Expected findings include:

- `PRIV-ACCESS-001`
- `PRIV-AUDIT-001`
- `PRIV-RETENTION-001`
- `PRIV-DISPOSAL-001`

`PRIV-API-001` findings indicate that personal data
is exposed through an API and remain visible
even after management controls are applied.

---

## 2. Managed state

Replace the current controller with:

`variants/MemberController.managed.java.txt`

and use:

`policies/managed.yml`

The managed variant introduces:

- access control via `@PreAuthorize`
- privacy audit declaration via `@PrivacyAudit`
- retention policy
- disposal policy

Expected result:

- `PRIV-ACCESS-001` removed
- `PRIV-AUDIT-001` removed
- `PRIV-RETENTION-001` removed
- `PRIV-DISPOSAL-001` removed

The API privacy findings remain because PrivacyOps
does not treat the existence of personal data as an error by itself.

Instead, PrivacyOps evaluates whether personal data flows
are visible and governed.