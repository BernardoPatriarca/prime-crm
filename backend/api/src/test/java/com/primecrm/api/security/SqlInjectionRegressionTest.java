package com.primecrm.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.primecrm.api.exception.GlobalExceptionHandler;
import com.primecrm.core.dto.commercial.CustomerListFilter;
import com.primecrm.core.dto.commercial.LeadListFilter;
import com.primecrm.core.service.CustomerService;
import com.primecrm.core.service.LeadService;
import com.primecrm.core.service.UserService;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.commercial.Customer;
import com.primecrm.infra.entity.commercial.Lead;
import com.primecrm.infra.entity.commercial.PersonType;
import com.primecrm.infra.repository.CustomerRepository;
import com.primecrm.infra.repository.LeadRepository;
import com.primecrm.infra.repository.UserRepository;
import com.primecrm.shared.dto.ApiErrorResponse;
import com.primecrm.shared.exception.BadRequestException;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@EnabledIf(value = "com.primecrm.api.performance.LocalPostgresCondition#isReachable",
        disabledReason = "Postgres local nao esta acessivel neste ambiente")
@Transactional
class SqlInjectionRegressionTest {

    private static final List<String> INJECTION_PAYLOADS = List.of(
            "'; DROP TABLE customers; --",
            "'; DROP TABLE leads; --",
            "'; DROP TABLE users; --",
            "\" OR \"1\"=\"1",
            "' OR '1'='1",
            "%' OR 1=1 --",
            "admin'--",
            "1; DELETE FROM users",
            "1 UNION SELECT null, null, null --",
            "') OR ('a'='a",
            "'; UPDATE users SET password_hash = 'x'; --",
            "\\'; DROP TABLE customers; --"
    );

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 20, Sort.by("name"));

    @Autowired
    private CustomerService customerService;
    @Autowired
    private LeadService leadService;
    @Autowired
    private UserService userService;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private LeadRepository leadRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void customerSearch_isImmuneToInjectionPayloads() {
        String token = seedCustomer();
        long customersBefore = countRows("Customer");
        long leadsBefore = countRows("Lead");
        long usersBefore = countRows("User");

        assertThat(searchCustomers(token).getTotalElements()).isEqualTo(1);

        for (String payload : INJECTION_PAYLOADS) {
            assertThatCode(() -> assertThat(searchCustomers(payload).getTotalElements())
                    .as("payload deveria retornar zero clientes: %s", payload)
                    .isZero())
                    .as("payload nao deveria lancar excecao: %s", payload)
                    .doesNotThrowAnyException();
        }

        assertTablesIntact(customersBefore, leadsBefore, usersBefore);
        assertThat(searchCustomers(token).getTotalElements()).isEqualTo(1);
    }

    @Test
    void leadSearch_isImmuneToInjectionPayloads() {
        String token = seedLead();
        long customersBefore = countRows("Customer");
        long leadsBefore = countRows("Lead");
        long usersBefore = countRows("User");

        assertThat(searchLeads(token).getTotalElements()).isEqualTo(1);

        for (String payload : INJECTION_PAYLOADS) {
            assertThatCode(() -> assertThat(searchLeads(payload).getTotalElements())
                    .as("payload deveria retornar zero leads: %s", payload)
                    .isZero())
                    .as("payload nao deveria lancar excecao: %s", payload)
                    .doesNotThrowAnyException();
        }

        assertTablesIntact(customersBefore, leadsBefore, usersBefore);
        assertThat(searchLeads(token).getTotalElements()).isEqualTo(1);
    }

    @Test
    void userSearch_isImmuneToInjectionPayloads() {
        String token = uniqueToken();
        seedUser(token);
        long customersBefore = countRows("Customer");
        long leadsBefore = countRows("Lead");
        long usersBefore = countRows("User");

        assertThat(searchUsers(token).getTotalElements()).isEqualTo(1);

        for (String payload : INJECTION_PAYLOADS) {
            assertThatCode(() -> assertThat(searchUsers(payload).getTotalElements())
                    .as("payload deveria retornar zero usuarios: %s", payload)
                    .isZero())
                    .as("payload nao deveria lancar excecao: %s", payload)
                    .doesNotThrowAnyException();
        }

        assertTablesIntact(customersBefore, leadsBefore, usersBefore);
        assertThat(searchUsers(token).getTotalElements()).isEqualTo(1);
    }

    @Test
    void searchTreatsInjectionLookalikeAsLiteralData() {
        String token = uniqueToken();
        Customer customer = new Customer();
        customer.setName(token + " O'Brien select from drop");
        customer.setPersonType(PersonType.FISICA);
        customerRepository.save(customer);
        entityManager.flush();

        assertThat(searchCustomers("O'Brien").getTotalElements()).isEqualTo(1);
        assertThat(searchCustomers("o'brien select from drop").getTotalElements()).isEqualTo(1);
        assertThat(searchCustomers(token + " O'Brien").getTotalElements()).isEqualTo(1);
    }

    @Test
    void unknownSortPropertyIsReportedAsBadRequestWithoutLeakingInternals() {
        CustomerListFilter filter = new CustomerListFilter(uniqueToken(), null, null, null, null, null, null);
        Pageable invalidSort = PageRequest.of(0, 20, Sort.by("campoInexistente"));

        Throwable thrown = catchThrowable(() -> customerService.list(filter, invalidSort));
        assertThat(thrown).isNotNull();

        ResponseEntity<ApiErrorResponse> response = handle(thrown);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.errorCode()).isEqualTo("INVALID_SORT_PROPERTY");
        assertThat(body.message()).doesNotContain("campoInexistente");
        assertThat(body.message()).doesNotContainIgnoringCase("select");
        assertThat(body.message()).doesNotContainIgnoringCase("customers");
    }

    @Test
    void sortBySensitivePropertyIsRejected() {
        assertThatThrownBy(() -> com.primecrm.api.support.SortGuard.requireSafeSort(
                PageRequest.of(0, 20, Sort.by("passwordHash"))))
                .isInstanceOf(BadRequestException.class);

        assertThatCode(() -> com.primecrm.api.support.SortGuard.requireSafeSort(FIRST_PAGE))
                .doesNotThrowAnyException();
    }

    private Page<?> searchCustomers(String search) {
        return customerService.list(new CustomerListFilter(search, null, null, null, null, null, null), FIRST_PAGE);
    }

    private Page<?> searchLeads(String search) {
        return leadService.list(
                new LeadListFilter(search, null, null, null, null, null, null, null, null, null), FIRST_PAGE);
    }

    private Page<?> searchUsers(String search) {
        return userService.list(search, null, FIRST_PAGE);
    }

    private String seedCustomer() {
        String token = uniqueToken();
        Customer customer = new Customer();
        customer.setName(token + "-cliente");
        customer.setPersonType(PersonType.JURIDICA);
        customerRepository.save(customer);
        entityManager.flush();
        return token;
    }

    private String seedLead() {
        String token = uniqueToken();
        Lead lead = new Lead();
        lead.setName(token + "-lead");
        leadRepository.save(lead);
        entityManager.flush();
        return token;
    }

    private void seedUser(String token) {
        User user = new User();
        user.setName(token + "-usuario");
        user.setEmail(token + "@sqlinjection.local");
        user.setLogin(token);
        user.setPasswordHash("not-a-real-hash");
        userRepository.save(user);
        entityManager.flush();
    }

    private void assertTablesIntact(long customersBefore, long leadsBefore, long usersBefore) {
        entityManager.flush();
        entityManager.clear();
        assertThat(countRows("Customer")).isEqualTo(customersBefore);
        assertThat(countRows("Lead")).isEqualTo(leadsBefore);
        assertThat(countRows("User")).isEqualTo(usersBefore);
        assertThat(customerRepository.count()).isEqualTo(customersBefore);
        assertThat(leadRepository.count()).isEqualTo(leadsBefore);
        assertThat(userRepository.count()).isEqualTo(usersBefore);
    }

    private long countRows(String entityName) {
        return entityManager.createQuery("select count(e) from " + entityName + " e", Long.class)
                .getSingleResult();
    }

    private ResponseEntity<ApiErrorResponse> handle(Throwable thrown) {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/customers");
        if (thrown instanceof org.springframework.data.mapping.PropertyReferenceException propertyReference) {
            return handler.handlePropertyReference(propertyReference, request);
        }
        if (thrown instanceof org.springframework.dao.InvalidDataAccessApiUsageException invalidUsage) {
            return handler.handleInvalidDataAccessApiUsage(invalidUsage, request);
        }
        return handler.handleGeneric(new Exception(thrown), request);
    }

    private static Throwable catchThrowable(Runnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Throwable throwable) {
            return throwable;
        }
    }

    private String uniqueToken() {
        return "sqli" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
