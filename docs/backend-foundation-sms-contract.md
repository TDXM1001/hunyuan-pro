# SMS Foundation Contract

## Purpose
The SMS module is a shared support capability. Business modules should call `SmsService` instead of talking to SMS vendors directly.

## Current Capability
- Template-driven send path through `templateCode` and `templateParams`.
- Direct `content` fallback when a template has not been created yet.
- Provider adapter seam through `SmsProvider`.
- Default mock provider for local, test, pre, and prod profiles.
- Redis idempotency protection through `idempotentKey`.
- Redis rate limit by `phone + templateCode`.
- Persistent send log in `t_sms_send_log`.
- Template storage in `t_sms_template`.
- Admin send-log query endpoint at `/sms/sendLog/query`.
- Provider exceptions, null responses, and empty success payloads are persisted as failed send logs.

## Business Send Rule
Business code should provide:
- `phone`: target phone number.
- `templateCode`: stable platform template code.
- `templateParams`: values used to render the template.
- `idempotentKey`: business event key, recommended format `module:scene:businessId`.

Business code should not:
- Store vendor credentials.
- Render provider-specific templates.
- Implement its own SMS rate limit.
- Write directly to `t_sms_send_log`.
- Expose generic public HTTP endpoints for arbitrary SMS sending.

## Default Runtime Settings
The default config is:

```yaml
sms:
  mode: mock
  idempotent-expire-seconds: 300
  rate-limit-seconds: 60
```

`mode=mock` means the provider logs the send request and returns a generated request id. It is safe for non-vendor environments and keeps the platform path verifiable.

## Remaining Production Work
- Add real provider adapters.
- Add provider callback handling.
- Add retry policy and retry counters.
- Add visible frontend pages for SMS template and send-log management.
- Add template approval/versioning if SMS content must be governed.
- Add integration tests with a fake provider bean.
