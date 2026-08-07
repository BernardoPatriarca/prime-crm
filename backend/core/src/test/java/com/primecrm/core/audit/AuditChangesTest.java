package com.primecrm.core.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.domain.DomainType;
import com.primecrm.infra.entity.domain.DomainValue;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuditChangesTest {

    @Test
    void snapshot_skipsIdentityAndFrameworkAuditColumns() {
        Pipeline pipeline = new Pipeline();
        pipeline.setId(UUID.randomUUID());
        pipeline.setName("Funil Padrao");
        pipeline.setCreatedAt(Instant.now());
        pipeline.setCreatedBy("admin");

        Map<String, Object> snapshot = AuditChanges.snapshot(pipeline);

        assertThat(snapshot).containsEntry("name", "Funil Padrao");
        assertThat(snapshot).doesNotContainKeys("id", "tenantId", "createdAt", "updatedAt", "createdBy", "updatedBy");
    }

    @Test
    void snapshot_replacesAssociatedEntityWithItsIdentifier() {
        UUID domainTypeId = UUID.randomUUID();
        DomainType domainType = new DomainType();
        domainType.setId(domainTypeId);
        domainType.setCode("CLIENT_TYPE");

        DomainValue domainValue = new DomainValue();
        domainValue.setId(UUID.randomUUID());
        domainValue.setDomainType(domainType);
        domainValue.setName("Pessoa Fisica");

        Map<String, Object> snapshot = AuditChanges.snapshot(domainValue);

        assertThat(snapshot).containsEntry("domainType", domainTypeId.toString());
    }

    @Test
    void snapshot_redactsSensitiveKeysNestedInJsonColumns() {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("apiToken", "abc-123");
        extra.put("label", "visivel");

        DomainValue domainValue = new DomainValue();
        domainValue.setId(UUID.randomUUID());
        domainValue.setExtra(extra);

        Object normalizedExtra = AuditChanges.snapshot(domainValue).get("extra");

        assertThat(normalizedExtra).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> extraAsMap = (Map<String, Object>) normalizedExtra;
        assertThat(extraAsMap).containsEntry("apiToken", AuditChanges.REDACTED);
        assertThat(extraAsMap).containsEntry("label", "visivel");
    }

    @Test
    void normalize_convertsBigDecimalAndCollectionsToJsonFriendlyValues() {
        assertThat(AuditChanges.normalize(new BigDecimal("25.50"))).isEqualTo("25.50");
        assertThat(AuditChanges.normalize(List.of("a", "b"))).isEqualTo(List.of("a", "b"));
        assertThat(AuditChanges.normalize(null)).isNull();
    }

    @Test
    void diff_returnsEmptyMapWhenNothingChanged() {
        PipelineStage stage = new PipelineStage();
        stage.setId(UUID.randomUUID());
        stage.setName("Prospeccao");
        stage.setDefaultProbability(new BigDecimal("10.00"));

        Map<String, Object> before = AuditChanges.snapshot(stage);
        Map<String, Object> after = AuditChanges.snapshot(stage);

        assertThat(AuditChanges.diff(before, after)).isEmpty();
    }

    @Test
    void diff_toleratesNullPreviousState() {
        Pipeline pipeline = new Pipeline();
        pipeline.setId(UUID.randomUUID());
        pipeline.setName("Funil Novo");

        Map<String, Object> diff = AuditChanges.diff(null, AuditChanges.snapshot(pipeline));

        assertThat(diff).containsKey("name");
        @SuppressWarnings("unchecked")
        Map<String, Object> nameChange = (Map<String, Object>) diff.get("name");
        assertThat(nameChange)
                .containsEntry(AuditChanges.OLD_KEY, null)
                .containsEntry(AuditChanges.NEW_KEY, "Funil Novo");
    }

    @Test
    void isSensitive_flagsPasswordTokenAndHashFields() {
        assertThat(AuditChanges.isSensitive("passwordHash")).isTrue();
        assertThat(AuditChanges.isSensitive("refreshToken")).isTrue();
        assertThat(AuditChanges.isSensitive("clientSecret")).isTrue();
        assertThat(AuditChanges.isSensitive("name")).isFalse();
    }

    @Test
    void entityName_stripsHibernateProxySuffix() {
        assertThat(AuditChanges.entityName(new Pipeline())).isEqualTo("Pipeline");
        assertThat(AuditChanges.entityName(null)).isEqualTo("Unknown");
    }
}
