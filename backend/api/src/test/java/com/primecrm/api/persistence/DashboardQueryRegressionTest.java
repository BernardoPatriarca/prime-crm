package com.primecrm.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.core.dto.dashboard.DashboardResponse;
import com.primecrm.core.service.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@EnabledIf(value = "com.primecrm.api.performance.LocalPostgresCondition#isReachable",
        disabledReason = "Postgres local nao esta acessivel neste ambiente")
class DashboardQueryRegressionTest {

    @Autowired
    private DashboardService dashboardService;

    @Test
    void load_runsEveryAggregationAgainstPostgres() {
        DashboardResponse response = dashboardService.load(null, null, null);

        assertThat(response.metrics()).isNotNull();
        assertThat(response.monthly()).hasSize(DashboardService.MONTHLY_SERIES_SIZE);
        assertThat(response.ranking()).hasSizeLessThanOrEqualTo(DashboardService.RANKING_SIZE);
        assertThat(response.tasks()).isNotNull();
    }

    @Test
    void load_withAnExplicitPeriod_keepsTheBoundariesInTheResponse() {
        LocalDate from = LocalDate.now().minusDays(90);
        LocalDate to = LocalDate.now();

        DashboardResponse response = dashboardService.load(from, to, null);

        assertThat(response.from()).isEqualTo(from);
        assertThat(response.to()).isEqualTo(to);
    }

    @Test
    void load_funnelStagesComeInDisplayOrderWithNonNegativeTotals() {
        DashboardResponse response = dashboardService.load(null, null, null);

        assertThat(response.funnel().stages()).isSortedAccordingTo(
                (first, second) -> Integer.compare(first.displayOrder(), second.displayOrder()));
        assertThat(response.funnel().stages())
                .allSatisfy(stage -> assertThat(stage.amount()).isGreaterThanOrEqualTo(BigDecimal.ZERO));
    }
}
