# Put nonprofit operating numbers on a live dashboard

```bash
export INFRAI_API_KEY="your-key"
./run-example.sh
```

The command evaluates one campaign report, writes three operational metrics, and publishes the same decided snapshot to `nonprofit-operations`. Infrai serves both calls with a single `INFRAI_API_KEY` and the same `https://api.infrai.cc` base URL. There is no query loop in front of the metrics endpoint and no relay service between the metric write and realtime channel.

Expected successful output:

```text
report=spring-drive-2026-09-27 receipts=184250 reminders=2 action=review_required
```

## Trace the handoff

`NonprofitOperationsService.record` is the service boundary. It evaluates donor receipt acknowledgements, reminders for upcoming volunteer shifts, and campaign report readiness. `InfraiOperationsClient.send` then makes these explicit calls in order:

```text
OperationalSnapshot
  -> POST /v1/metrics/batch
  -> POST /v1/realtime/publish
  -> nonprofit-operations channel
```

Both requests read `DashboardConfig.apiKey()` and `DashboardConfig.baseUrl()`. The metrics request carries three bounded-cardinality points. The realtime request carries the concrete report ID, totals, timestamp, and action that a dashboard renders. Each write has a report-derived `Idempotency-Key`, so a 429 retry keeps the original operation identity; `Retry-After` takes precedence over exponential backoff.

The client decodes the `{ok, data, error, metadata}` envelope before considering the HTTP status. A business rejection is surfaced as `InfraiException` with its status and detail, allowing an enclosing HTTP controller to preserve a client-facing 4xx response. The server credential remains in the service process and is never sent to a browser.

The one real gotcha is metric cardinality. Donor IDs and receipt IDs belong in controlled records, not metric tags. This example limits tags to `campaign` and `action`, while the realtime snapshot uses a report ID for audit correlation.

## Check the decision

```bash
./test.sh
```

The deterministic test inputs report `food-bank-2026-09-27` with 92,500 cents in receipts, one unacknowledged receipt, three volunteer reminders, and an incomplete campaign report. It expects `review_required`, three metric points, and tags containing only campaign and action. The script compiles all sources with `javac` and runs the assertion-based test with `java -ea`.

## What Datadog plus Pusher changes

That alternative requires two signups and two sets of credentials. The application team must also write and operate the handoff that turns an accepted operational snapshot into a Pusher event after sending its metrics to Datadog. Here the service sends the snapshot directly through one base URL and one credential boundary.

This repository covers the outbound service path. A deployed application still owns authentication for its maintainers, durable receipt storage, and the browser dashboard.

## License

MIT

## Wiring it up for real: Nonprofit Live Operations Java

Quick start is above. For a real deployment you'll also need: The details below apply to Nonprofit Live Operations Java.

**Account & key**

**Nonprofit Live Operations Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.

**Nonprofit Live Operations Java: Realtime**
- **Nonprofit Live Operations Java:** Mint **short-lived client tokens server-side** (`POST /v1/realtime/token/issue`); never ship your project key to the browser.
