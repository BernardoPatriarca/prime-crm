package com.primecrm.api.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.core.dto.commercial.CustomerListFilter;
import com.primecrm.core.dto.commercial.OpportunityListFilter;
import com.primecrm.core.service.CustomerService;
import com.primecrm.core.service.OpportunityService;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.CustomerTag;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.PersonType;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.CustomerTagRepository;
import com.primecrm.infra.repository.DomainValueRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.infra.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("dev")
@EnabledIf(value = "com.primecrm.api.performance.LocalPostgresCondition#isReachable",
        disabledReason = "Postgres local nao esta acessivel neste ambiente")
@Transactional
class CommercialListQueryCountRegressionTest {

    private static final int SMALL_SAMPLE = 2;
    private static final int LARGE_SAMPLE = 20;
    private static final int TAGS_PER_CUSTOMER = 2;

    @Autowired
    private CustomerService customerService;
    @Autowired
    private OpportunityService opportunityService;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private CustomerTagRepository customerTagRepository;
    @Autowired
    private OpportunityRepository opportunityRepository;
    @Autowired
    private PipelineRepository pipelineRepository;
    @Autowired
    private PipelineStageRepository pipelineStageRepository;
    @Autowired
    private DomainValueRepository domainValueRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void listCustomers_queryCountDoesNotGrowWithRowCount() {
        long small = measureCustomerList(SMALL_SAMPLE);
        long large = measureCustomerList(LARGE_SAMPLE);

        report("customers", small, large);
        assertThat(large).isEqualTo(small);
    }

    @Test
    void listOpportunities_queryCountDoesNotGrowWithRowCount() {
        long small = measureOpportunityList(SMALL_SAMPLE);
        long large = measureOpportunityList(LARGE_SAMPLE);

        report("opportunities", small, large);
        assertThat(large).isEqualTo(small);
    }

    private long measureCustomerList(int rows) {
        String token = seedCustomers(rows);
        CustomerListFilter filter = new CustomerListFilter(token, null, null, null, null, null, null);
        return countQueries(() -> {
            var page = customerService.list(filter, PageRequest.of(0, 50, Sort.by("name")));
            assertThat(page.getTotalElements()).isEqualTo(rows);
            var first = page.getContent().get(0);
            assertThat(first.tags()).hasSize(TAGS_PER_CUSTOMER);
            assertThat(first.clientType()).isNotNull();
            assertThat(first.segment()).isNotNull();
            assertThat(first.origin()).isNotNull();
            assertThat(first.owner()).isNotNull();
            assertThat(first.code()).startsWith("CLI-");
            return page;
        });
    }

    private long measureOpportunityList(int rows) {
        String token = seedOpportunities(rows);
        OpportunityListFilter filter = new OpportunityListFilter(token, null, null, null, null, null,
                null, null, null, null);
        return countQueries(() -> {
            var page = opportunityService.list(filter, PageRequest.of(0, 50, Sort.by("title")));
            assertThat(page.getTotalElements()).isEqualTo(rows);
            var first = page.getContent().get(0);
            assertThat(first.customer()).isNotNull();
            assertThat(first.pipeline()).isNotNull();
            assertThat(first.stage()).isNotNull();
            assertThat(first.owner()).isNotNull();
            assertThat(first.team()).isNotNull();
            assertThat(first.code()).startsWith("OPO-");
            return page;
        });
    }

    private String seedCustomers(int rows) {
        String token = uniqueToken();
        List<DomainValue> domainValues = someDomainValues(3 + TAGS_PER_CUSTOMER);
        User owner = newUser(token);

        for (int i = 0; i < rows; i++) {
            Customer customer = new Customer();
            customer.setName(token + "-cliente-" + i);
            customer.setPersonType(PersonType.JURIDICA);
            customer.setClientType(domainValues.get(0));
            customer.setSegment(domainValues.get(1));
            customer.setOrigin(domainValues.get(2));
            customer.setOwner(owner);
            customer = customerRepository.save(customer);

            for (int t = 0; t < TAGS_PER_CUSTOMER; t++) {
                CustomerTag tag = new CustomerTag();
                tag.setCustomer(customer);
                tag.setDomainValue(domainValues.get(3 + t));
                customerTagRepository.save(tag);
            }
        }
        return token;
    }

    private String seedOpportunities(int rows) {
        String token = uniqueToken();
        List<DomainValue> domainValues = someDomainValues(1);
        User owner = newUser(token);

        Customer customer = new Customer();
        customer.setName(token + "-cliente-da-oportunidade");
        customer.setPersonType(PersonType.JURIDICA);
        customer = customerRepository.save(customer);

        Pipeline pipeline = new Pipeline();
        pipeline.setName(token + "-funil");
        pipeline.setBusinessType("SERVICES");
        pipeline = pipelineRepository.save(pipeline);

        PipelineStage stage = new PipelineStage();
        stage.setPipeline(pipeline);
        stage.setName("Qualificacao");
        stage.setDisplayOrder(1);
        stage.setDefaultProbability(new BigDecimal("30.00"));
        stage = pipelineStageRepository.save(stage);

        for (int i = 0; i < rows; i++) {
            Opportunity opportunity = new Opportunity();
            opportunity.setTitle(token + "-negocio-" + i);
            opportunity.setCustomer(customer);
            opportunity.setPipeline(pipeline);
            opportunity.setStage(stage);
            opportunity.setOwner(owner);
            opportunity.setTeam(domainValues.get(0));
            opportunity.setAmount(new BigDecimal("1000.00"));
            opportunityRepository.save(opportunity);
        }
        return token;
    }

    private User newUser(String token) {
        User user = new User();
        user.setName(token + "-responsavel");
        user.setEmail(token + "@querycount.local");
        user.setLogin(token);
        user.setPasswordHash("not-a-real-hash");
        return userRepository.save(user);
    }

    private List<DomainValue> someDomainValues(int amount) {
        List<DomainValue> values = domainValueRepository.findAll().stream()
                .filter(value -> value.getDeletedAt() == null)
                .limit(amount)
                .toList();
        assertThat(values).hasSize(amount);
        return values;
    }

    private long countQueries(Supplier<?> action) {
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = statistics();
        statistics.clear();
        action.get();
        return statistics.getPrepareStatementCount();
    }

    private Statistics statistics() {
        return entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    }

    private String uniqueToken() {
        return "qc" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private void report(String scenario, long small, long large) {
        System.out.printf("[query-count] %s: %d rows -> %d queries | %d rows -> %d queries%n",
                scenario, SMALL_SAMPLE, small, LARGE_SAMPLE, large);
    }
}
