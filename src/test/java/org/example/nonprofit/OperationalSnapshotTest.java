package org.example.nonprofit;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class OperationalSnapshotTest {
    public static void main(String[] args) {
        OperationalSnapshot snapshot = OperationalSnapshot.assess(
                "food-bank-2026-09-27", "food-bank", 92_500, 1, 3, false,
                Instant.parse("2026-09-27T10:15:00Z"));

        assert snapshot.action().equals("review_required") : snapshot.action();
        assert snapshot.volunteerRemindersDue() == 3;
        List<Map<String, Object>> points = snapshot.metricPoints();
        assert points.size() == 3;
        assert points.get(1).get("value").equals(3);
        assert points.stream().allMatch(point -> {
            Map<?, ?> tags = (Map<?, ?>) point.get("tags");
            return tags.keySet().equals(java.util.Set.of("campaign", "action"));
        }) : "Metric tags must remain bounded";
        assert snapshot.dashboardData().get("report_id").equals("food-bank-2026-09-27");
        System.out.println("OperationalSnapshotTest passed");
    }
}
