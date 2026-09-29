package org.example.nonprofit;

import java.io.IOException;
import java.time.Instant;

public final class NonprofitOperationsService {
    private final InfraiOperationsClient client;

    public NonprofitOperationsService(InfraiOperationsClient client) {
        this.client = client;
    }

    public OperationalSnapshot record(
            String reportId,
            String campaign,
            long donorReceiptsCents,
            int unacknowledgedReceipts,
            int volunteerRemindersDue,
            boolean campaignReportReady,
            Instant observedAt) throws IOException, InterruptedException {
        OperationalSnapshot snapshot = OperationalSnapshot.assess(
                reportId, campaign, donorReceiptsCents, unacknowledgedReceipts,
                volunteerRemindersDue, campaignReportReady, observedAt);
        client.send(snapshot);
        return snapshot;
    }
}
