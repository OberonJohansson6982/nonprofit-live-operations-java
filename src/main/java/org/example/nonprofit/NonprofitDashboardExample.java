package org.example.nonprofit;

import java.time.Instant;

public final class NonprofitDashboardExample {
    private NonprofitDashboardExample() {}

    public static void main(String[] args) throws Exception {
        DashboardConfig config = DashboardConfig.fromEnvironment();
        NonprofitOperationsService service = new NonprofitOperationsService(
                new InfraiOperationsClient(config));
        OperationalSnapshot snapshot = service.record(
                "spring-drive-2026-09-27",
                "spring-drive",
                184_250,
                0,
                2,
                true,
                Instant.parse("2026-09-27T09:00:00Z"));
        System.out.printf("report=%s receipts=%d reminders=%d action=%s%n",
                snapshot.reportId(), snapshot.donorReceiptsCents(),
                snapshot.volunteerRemindersDue(), snapshot.action());
    }
}
