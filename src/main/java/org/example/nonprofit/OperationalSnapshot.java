package org.example.nonprofit;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record OperationalSnapshot(
        String reportId,
        String campaign,
        long donorReceiptsCents,
        int volunteerRemindersDue,
        boolean campaignReportReady,
        Instant observedAt,
        String action) {

    public static OperationalSnapshot assess(
            String reportId,
            String campaign,
            long donorReceiptsCents,
            int unacknowledgedReceipts,
            int volunteerRemindersDue,
            boolean campaignReportReady,
            Instant observedAt) {
        if (donorReceiptsCents < 0 || unacknowledgedReceipts < 0 || volunteerRemindersDue < 0) {
            throw new IllegalArgumentException("Operational counts cannot be negative");
        }
        String action = unacknowledgedReceipts > 0 || volunteerRemindersDue > 0
                ? "review_required"
                : campaignReportReady ? "report_ready" : "monitor";
        return new OperationalSnapshot(reportId, campaign, donorReceiptsCents,
                volunteerRemindersDue, campaignReportReady, observedAt, action);
    }

    public List<Map<String, Object>> metricPoints() {
        Map<String, String> tags = Map.of("campaign", campaign, "action", action);
        return List.of(
                point("nonprofit.donor_receipts_cents", donorReceiptsCents, "gauge", tags),
                point("nonprofit.volunteer_reminders_due", volunteerRemindersDue, "gauge", tags),
                point("nonprofit.campaign_report_ready", campaignReportReady ? 1 : 0, "gauge", tags));
    }

    public Map<String, Object> dashboardData() {
        return Map.of(
                "report_id", reportId,
                "campaign", campaign,
                "donor_receipts_cents", donorReceiptsCents,
                "volunteer_reminders_due", volunteerRemindersDue,
                "campaign_report_ready", campaignReportReady,
                "observed_at", observedAt.toString(),
                "action", action);
    }

    private Map<String, Object> point(
            String name, Number value, String type, Map<String, String> tags) {
        return Map.of("name", name, "value", value, "type", type, "tags", tags,
                "timestamp", observedAt.toString());
    }
}
