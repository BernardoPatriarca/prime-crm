package com.primecrm.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.core.dto.notification.NotificationListResponse;
import com.primecrm.core.dto.search.GlobalSearchResponse;
import com.primecrm.core.dto.search.SearchResultType;
import com.primecrm.core.service.GlobalSearchService;
import com.primecrm.core.service.NotificationService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@EnabledIf(value = "com.primecrm.api.performance.LocalPostgresCondition#isReachable",
        disabledReason = "Postgres local nao esta acessivel neste ambiente")
class TopbarQueryRegressionTest {

    private static final Set<String> ALL_PERMISSIONS = Set.of("CLIENTES_VIEW", "CONTATOS_VIEW", "LEADS_VIEW",
            "OPORTUNIDADES_VIEW", "TAREFAS_VIEW");

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private GlobalSearchService globalSearchService;

    @Test
    void notifications_runEveryDerivedQueryAgainstPostgres() {
        NotificationListResponse response = notificationService.list(UUID.randomUUID());

        assertThat(response.items()).hasSizeLessThanOrEqualTo(NotificationService.MAX_ITEMS);
        assertThat(response.total()).isGreaterThanOrEqualTo(response.items().size());
    }

    @Test
    void notifications_withoutAUser_stillReturnsTheUnassignedLeadAlerts() {
        assertThat(notificationService.list(null).items())
                .allSatisfy(item -> assertThat(item.link()).isEqualTo("/leads"));
    }

    @Test
    void search_belowTheMinimumLength_returnsNothing() {
        GlobalSearchResponse response = globalSearchService.search("a", ALL_PERMISSIONS::contains);

        assertThat(response.results()).isEmpty();
        assertThat(response.total()).isZero();
    }

    @Test
    void search_findsRecordsAcrossModules() {
        GlobalSearchResponse response = globalSearchService.search("a", ALL_PERMISSIONS::contains);
        GlobalSearchResponse wide = globalSearchService.search("li", ALL_PERMISSIONS::contains);

        assertThat(response.results()).isEmpty();
        assertThat(wide.results()).hasSizeLessThanOrEqualTo(
                GlobalSearchService.RESULTS_PER_TYPE * SearchResultType.values().length);
        assertThat(wide.results()).allSatisfy(result -> {
            assertThat(result.title()).isNotBlank();
            assertThat(result.link()).startsWith("/");
        });
    }

    @Test
    void search_withoutPermission_skipsTheModule() {
        GlobalSearchResponse response = globalSearchService.search("li", permission -> false);

        assertThat(response.results()).isEmpty();
    }
}
