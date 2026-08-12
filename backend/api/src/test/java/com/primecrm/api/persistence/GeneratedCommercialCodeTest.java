package com.primecrm.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.api.config.JpaConfig;
import com.primecrm.infra.config.JpaAuditingConfig;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.PersonType;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("dev")
@Import({JpaConfig.class, JpaAuditingConfig.class})
@EnabledIf(value = "com.primecrm.api.performance.LocalPostgresCondition#isReachable",
        disabledReason = "Postgres local nao esta acessivel neste ambiente")
class GeneratedCommercialCodeTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private OpportunityRepository opportunityRepository;
    @Autowired
    private PipelineRepository pipelineRepository;
    @Autowired
    private PipelineStageRepository pipelineStageRepository;

    @Test
    void customerCode_isFilledByTheDatabaseSequenceRightAfterSave() {
        Customer customer = customerRepository.save(newCustomer());
        entityManager.flush();

        assertThat(customer.getCode()).isNotBlank().startsWith("CLI-");
        assertThat(customer.getCode()).matches("CLI-\\d{6,}");
    }

    @Test
    void leadCode_isFilledByTheDatabaseSequenceRightAfterSave() {
        Lead lead = new Lead();
        lead.setName("Lead " + UUID.randomUUID());
        lead = leadRepository.save(lead);
        entityManager.flush();

        assertThat(lead.getCode()).isNotBlank().matches("LEAD-\\d{6,}");
    }

    @Test
    void opportunityCode_isFilledByTheDatabaseSequenceRightAfterSave() {
        Customer customer = customerRepository.save(newCustomer());
        Pipeline pipeline = new Pipeline();
        pipeline.setName("Funil " + UUID.randomUUID());
        pipeline.setBusinessType("SERVICES");
        pipeline = pipelineRepository.save(pipeline);

        PipelineStage stage = new PipelineStage();
        stage.setPipeline(pipeline);
        stage.setName("Qualificacao");
        stage.setDisplayOrder(1);
        stage.setDefaultProbability(BigDecimal.ZERO);
        stage = pipelineStageRepository.save(stage);

        Opportunity opportunity = new Opportunity();
        opportunity.setTitle("Negocio " + UUID.randomUUID());
        opportunity.setCustomer(customer);
        opportunity.setPipeline(pipeline);
        opportunity.setStage(stage);
        opportunity = opportunityRepository.save(opportunity);
        entityManager.flush();

        assertThat(opportunity.getCode()).isNotBlank().matches("OPO-\\d{6,}");
    }

    @Test
    void codeSuppliedByTheApplicationIsIgnored_theSequenceValueWins() {
        Customer customer = newCustomer();
        customer.setCode("CLI-CHUTADO");
        customer = customerRepository.save(customer);
        entityManager.flush();

        assertThat(customer.getCode()).isNotEqualTo("CLI-CHUTADO");
        assertThat(customer.getCode()).matches("CLI-\\d{6,}");
    }

    @Test
    void generatedCodesAreUniqueAcrossConsecutiveInserts() {
        Customer first = customerRepository.save(newCustomer());
        Customer second = customerRepository.save(newCustomer());
        entityManager.flush();

        assertThat(first.getCode()).isNotEqualTo(second.getCode());
    }

    private Customer newCustomer() {
        Customer customer = new Customer();
        customer.setName("Cliente " + UUID.randomUUID());
        customer.setPersonType(PersonType.JURIDICA);
        return customer;
    }
}
