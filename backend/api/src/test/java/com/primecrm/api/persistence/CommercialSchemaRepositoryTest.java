package com.primecrm.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.api.config.JpaConfig;
import com.primecrm.infra.config.JpaAuditingConfig;
import com.primecrm.infra.entity.commercial.Contact;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.CustomerTag;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.LeadTag;
import com.primecrm.infra.entity.commercial.Opportunity;
import com.primecrm.infra.entity.commercial.OpportunityOutcome;
import com.primecrm.infra.entity.commercial.OpportunityStageHistory;
import com.primecrm.infra.entity.commercial.PersonType;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.domain.DomainValue;
import com.primecrm.infra.repository.ContactRepository;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.CustomerTagRepository;
import com.primecrm.infra.repository.DomainValueRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.LeadTagRepository;
import com.primecrm.infra.repository.OpportunityRepository;
import com.primecrm.infra.repository.OpportunityStageHistoryRepository;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
class CommercialSchemaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ContactRepository contactRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private OpportunityRepository opportunityRepository;
    @Autowired
    private OpportunityStageHistoryRepository opportunityStageHistoryRepository;
    @Autowired
    private CustomerTagRepository customerTagRepository;
    @Autowired
    private LeadTagRepository leadTagRepository;
    @Autowired
    private PipelineRepository pipelineRepository;
    @Autowired
    private PipelineStageRepository pipelineStageRepository;
    @Autowired
    private DomainValueRepository domainValueRepository;

    @Test
    void savesCustomerWithContactsAndReadsThemBack() {
        Customer customer = persistCustomer(PersonType.JURIDICA);
        persistContact(customer, "Maria Compradora", true);
        persistContact(customer, "Joao Tecnico", false);
        flushAndClear();

        Customer reloaded = customerRepository.findByIdAndDeletedAtIsNull(customer.getId()).orElseThrow();
        assertThat(reloaded.getPersonType()).isEqualTo(PersonType.JURIDICA);
        assertThat(reloaded.getCountry()).isEqualTo("Brasil");
        assertThat(reloaded.getCreditLimit()).isEqualByComparingTo("15000.00");
        assertThat(reloaded.getHealthScore()).isEqualTo(80);
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getTenantId()).isNotNull();

        List<Contact> contacts = contactRepository
                .findByCustomer_IdAndDeletedAtIsNullOrderByNameAsc(customer.getId());
        assertThat(contacts).extracting(Contact::getName)
                .containsExactly("Joao Tecnico", "Maria Compradora");
        assertThat(contactRepository.countByCustomer_IdAndDeletedAtIsNull(customer.getId())).isEqualTo(2);
    }

    @Test
    void savesOpportunityWithStageHistory() {
        Customer customer = persistCustomer(PersonType.JURIDICA);
        Contact contact = persistContact(customer, "Maria Compradora", true);
        Pipeline pipeline = persistPipeline();
        PipelineStage first = persistStage(pipeline, "Qualificacao", 1);
        PipelineStage second = persistStage(pipeline, "Proposta", 2);

        Opportunity opportunity = new Opportunity();
        opportunity.setCode(uniqueCode("OPP"));
        opportunity.setTitle("Implantacao ERP");
        opportunity.setCustomer(customer);
        opportunity.setContact(contact);
        opportunity.setPipeline(pipeline);
        opportunity.setStage(second);
        opportunity.setAmount(new BigDecimal("42000.00"));
        opportunity.setProbability(new BigDecimal("60.00"));
        opportunity.setExpectedCloseDate(LocalDate.now().plusDays(30));
        opportunity.setCompetitor("Concorrente X");
        opportunityRepository.save(opportunity);

        Instant openedAt = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        persistStageHistory(opportunity, null, first, openedAt.minusSeconds(600), null);
        persistStageHistory(opportunity, first, second, openedAt, 4);
        flushAndClear();

        Opportunity reloaded = opportunityRepository.findByIdAndDeletedAtIsNull(opportunity.getId()).orElseThrow();
        assertThat(reloaded.getOutcome()).isEqualTo(OpportunityOutcome.OPEN);
        assertThat(reloaded.getOpenedAt()).isNotNull();
        assertThat(reloaded.getAmount()).isEqualByComparingTo("42000.00");
        assertThat(reloaded.getCustomer().getId()).isEqualTo(customer.getId());
        assertThat(reloaded.getStage().getName()).isEqualTo("Proposta");

        List<OpportunityStageHistory> history = opportunityStageHistoryRepository
                .findByOpportunity_IdAndDeletedAtIsNullOrderByMovedAtAsc(opportunity.getId());
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getFromStage()).isNull();
        assertThat(history.get(0).getToStage().getId()).isEqualTo(first.getId());
        assertThat(history.get(1).getFromStage().getId()).isEqualTo(first.getId());
        assertThat(history.get(1).getDaysInPreviousStage()).isEqualTo(4);

        OpportunityStageHistory latest = opportunityStageHistoryRepository
                .findFirstByOpportunity_IdAndDeletedAtIsNullOrderByMovedAtDesc(opportunity.getId());
        assertThat(latest.getToStage().getId()).isEqualTo(second.getId());
    }

    @Test
    void batchLookupsReturnRowsForEveryRequestedParent() {
        Customer firstCustomer = persistCustomer(PersonType.JURIDICA);
        Customer secondCustomer = persistCustomer(PersonType.FISICA);
        persistContact(firstCustomer, "Contato A", true);
        persistContact(firstCustomer, "Contato B", false);
        persistContact(secondCustomer, "Contato C", true);

        Pipeline pipeline = persistPipeline();
        PipelineStage stage = persistStage(pipeline, "Qualificacao", 1);
        Opportunity firstOpportunity = persistOpportunity(firstCustomer, pipeline, stage);
        Opportunity secondOpportunity = persistOpportunity(secondCustomer, pipeline, stage);
        persistStageHistory(firstOpportunity, null, stage, Instant.now(), null);
        persistStageHistory(secondOpportunity, null, stage, Instant.now(), null);

        DomainValue tag = anyDomainValue();
        persistCustomerTag(firstCustomer, tag);
        persistCustomerTag(secondCustomer, tag);

        Lead lead = persistLead(firstCustomer);
        persistLeadTag(lead, tag);
        flushAndClear();

        List<UUID> customerIds = List.of(firstCustomer.getId(), secondCustomer.getId());

        assertThat(contactRepository.findByCustomer_IdInAndDeletedAtIsNull(customerIds)).hasSize(3);
        assertThat(customerTagRepository.findByCustomer_IdIn(customerIds)).hasSize(2);
        assertThat(customerRepository.findByIdInAndDeletedAtIsNull(customerIds)).hasSize(2);
        assertThat(opportunityRepository.findByCustomer_IdInAndDeletedAtIsNull(customerIds)).hasSize(2);
        assertThat(opportunityStageHistoryRepository
                .findByOpportunity_IdInAndDeletedAtIsNullOrderByMovedAtAsc(
                        List.of(firstOpportunity.getId(), secondOpportunity.getId())))
                .hasSize(2);
        assertThat(leadTagRepository.findByLead_IdIn(List.of(lead.getId()))).hasSize(1);
        assertThat(leadRepository.findByConvertedCustomer_IdInAndDeletedAtIsNull(customerIds))
                .extracting(Lead::getId)
                .containsExactly(lead.getId());
    }

    @Test
    void codeUniquenessHelpersSeeThePersistedRow() {
        Customer customer = persistCustomer(PersonType.FISICA);
        flushAndClear();

        assertThat(customerRepository.existsByCodeAndDeletedAtIsNull(customer.getCode())).isTrue();
        assertThat(customerRepository
                .existsByCodeAndIdNotAndDeletedAtIsNull(customer.getCode(), customer.getId())).isFalse();
        assertThat(customerRepository.existsByDocumentAndDeletedAtIsNull(customer.getDocument())).isTrue();
    }

    private Customer persistCustomer(PersonType personType) {
        Customer customer = new Customer();
        customer.setCode(uniqueCode("CLI"));
        customer.setName("Cliente " + UUID.randomUUID());
        customer.setTradeName("Fantasia");
        customer.setPersonType(personType);
        customer.setDocument(uniqueDocument());
        customer.setEmail("contato@cliente.local");
        customer.setCity("Sao Paulo");
        customer.setState("SP");
        customer.setLatitude(new BigDecimal("-23.5505199"));
        customer.setLongitude(new BigDecimal("-46.6333094"));
        customer.setBirthDate(LocalDate.of(2005, 3, 14));
        customer.setCreditLimit(new BigDecimal("15000.00"));
        customer.setPaymentTerms("30/60/90");
        customer.setHealthScore(80);
        customer.setNotes("Cliente criado pelo teste de schema.");
        return customerRepository.save(customer);
    }

    private Contact persistContact(Customer customer, String name, boolean primary) {
        Contact contact = new Contact();
        contact.setCustomer(customer);
        contact.setName(name);
        contact.setPositionTitle("Gerente de Compras");
        contact.setEmail(name.replace(' ', '.').toLowerCase() + "@cliente.local");
        contact.setBirthDate(LocalDate.of(1988, 7, 1));
        contact.setPrimaryContact(primary);
        contact.setDecisionMaker(primary);
        return contactRepository.save(contact);
    }

    private Lead persistLead(Customer convertedCustomer) {
        Lead lead = new Lead();
        lead.setCode(uniqueCode("LEAD"));
        lead.setName("Lead " + UUID.randomUUID());
        lead.setCompanyName("Empresa Prospect");
        lead.setEmail("prospect@lead.local");
        lead.setProbability(new BigDecimal("25.00"));
        lead.setEstimatedValue(new BigDecimal("9000.00"));
        lead.setQualificationScore(55);
        lead.setExpectedCloseDate(LocalDate.now().plusDays(15));
        lead.setConvertedCustomer(convertedCustomer);
        lead.setConvertedAt(Instant.now());
        return leadRepository.save(lead);
    }

    private Opportunity persistOpportunity(Customer customer, Pipeline pipeline, PipelineStage stage) {
        Opportunity opportunity = new Opportunity();
        opportunity.setCode(uniqueCode("OPP"));
        opportunity.setTitle("Negocio " + UUID.randomUUID());
        opportunity.setCustomer(customer);
        opportunity.setPipeline(pipeline);
        opportunity.setStage(stage);
        opportunity.setAmount(new BigDecimal("1000.00"));
        return opportunityRepository.save(opportunity);
    }

    private OpportunityStageHistory persistStageHistory(Opportunity opportunity, PipelineStage from,
            PipelineStage to, Instant movedAt, Integer daysInPreviousStage) {
        OpportunityStageHistory history = new OpportunityStageHistory();
        history.setOpportunity(opportunity);
        history.setFromStage(from);
        history.setToStage(to);
        history.setMovedAt(movedAt);
        history.setDaysInPreviousStage(daysInPreviousStage);
        history.setNote("movimentacao de teste");
        return opportunityStageHistoryRepository.save(history);
    }

    private CustomerTag persistCustomerTag(Customer customer, DomainValue tag) {
        CustomerTag customerTag = new CustomerTag();
        customerTag.setCustomer(customer);
        customerTag.setDomainValue(tag);
        return customerTagRepository.save(customerTag);
    }

    private LeadTag persistLeadTag(Lead lead, DomainValue tag) {
        LeadTag leadTag = new LeadTag();
        leadTag.setLead(lead);
        leadTag.setDomainValue(tag);
        return leadTagRepository.save(leadTag);
    }

    private Pipeline persistPipeline() {
        Pipeline pipeline = new Pipeline();
        pipeline.setName("Funil " + UUID.randomUUID());
        pipeline.setBusinessType("SERVICES");
        return pipelineRepository.save(pipeline);
    }

    private PipelineStage persistStage(Pipeline pipeline, String name, int order) {
        PipelineStage stage = new PipelineStage();
        stage.setPipeline(pipeline);
        stage.setName(name);
        stage.setDisplayOrder(order);
        stage.setDefaultProbability(BigDecimal.ZERO);
        return pipelineStageRepository.save(stage);
    }

    private DomainValue anyDomainValue() {
        return domainValueRepository.findAll().stream()
                .filter(value -> value.getDeletedAt() == null)
                .findFirst()
                .orElseThrow();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private String uniqueCode(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private String uniqueDocument() {
        return String.format("%014d", Math.abs(UUID.randomUUID().getMostSignificantBits() % 100_000_000_000_000L));
    }
}
