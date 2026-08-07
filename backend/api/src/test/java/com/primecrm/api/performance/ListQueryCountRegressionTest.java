package com.primecrm.api.performance;

import static org.assertj.core.api.Assertions.assertThat;

import com.primecrm.core.service.PipelineService;
import com.primecrm.core.service.RoleService;
import com.primecrm.core.service.UserService;
import com.primecrm.infra.entity.auth.Permission;
import com.primecrm.infra.entity.auth.Role;
import com.primecrm.infra.entity.auth.RolePermission;
import com.primecrm.infra.entity.auth.User;
import com.primecrm.infra.entity.config.Pipeline;
import com.primecrm.infra.entity.config.PipelineStage;
import com.primecrm.infra.entity.auth.UserRole;
import com.primecrm.infra.repository.PermissionRepository;
import com.primecrm.infra.repository.PipelineRepository;
import com.primecrm.infra.repository.PipelineStageRepository;
import com.primecrm.infra.repository.RolePermissionRepository;
import com.primecrm.infra.repository.RoleRepository;
import com.primecrm.infra.repository.UserRepository;
import com.primecrm.infra.repository.UserRoleRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.ArrayList;
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
class ListQueryCountRegressionTest {

    private static final int SMALL_SAMPLE = 2;
    private static final int LARGE_SAMPLE = 10;
    private static final int STAGES_PER_PIPELINE = 3;
    private static final int PERMISSIONS_PER_ROLE = 3;
    private static final int ROLES_PER_USER = 2;

    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private UserService userService;
    @Autowired
    private PipelineRepository pipelineRepository;
    @Autowired
    private PipelineStageRepository pipelineStageRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private RolePermissionRepository rolePermissionRepository;
    @Autowired
    private PermissionRepository permissionRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserRoleRepository userRoleRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void listPipelines_queryCountDoesNotGrowWithRowCount() {
        long small = measurePipelineList(SMALL_SAMPLE);
        long large = measurePipelineList(LARGE_SAMPLE);

        report("pipelines", small, large);
        assertThat(large).isEqualTo(small);
    }

    @Test
    void listRoles_queryCountDoesNotGrowWithRowCount() {
        long small = measureRoleList(SMALL_SAMPLE);
        long large = measureRoleList(LARGE_SAMPLE);

        report("roles", small, large);
        assertThat(large).isEqualTo(small);
    }

    @Test
    void listUsers_queryCountDoesNotGrowWithRowCount() {
        long small = measureUserList(SMALL_SAMPLE);
        long large = measureUserList(LARGE_SAMPLE);

        report("users", small, large);
        assertThat(large).isEqualTo(small);
    }

    private long measurePipelineList(int rows) {
        String token = seedPipelines(rows);
        return countQueries(() -> {
            var page = pipelineService.list(token, null, PageRequest.of(0, 50, Sort.by("name")));
            assertThat(page.getTotalElements()).isEqualTo(rows);
            assertThat(page.getContent().get(0).stages()).hasSize(STAGES_PER_PIPELINE);
            return page;
        });
    }

    private long measureRoleList(int rows) {
        String token = seedRoles(rows);
        return countQueries(() -> {
            var page = roleService.list(token, null, PageRequest.of(0, 50, Sort.by("name")));
            assertThat(page.getTotalElements()).isEqualTo(rows);
            assertThat(page.getContent().get(0).permissions()).hasSize(PERMISSIONS_PER_ROLE);
            return page;
        });
    }

    private long measureUserList(int rows) {
        String token = seedUsers(rows);
        return countQueries(() -> {
            var page = userService.list(token, null, PageRequest.of(0, 50, Sort.by("name")));
            assertThat(page.getTotalElements()).isEqualTo(rows);
            assertThat(page.getContent().get(0).roles()).hasSize(ROLES_PER_USER);
            return page;
        });
    }

    private String seedPipelines(int rows) {
        String token = uniqueToken();
        for (int i = 0; i < rows; i++) {
            Pipeline pipeline = new Pipeline();
            pipeline.setName(token + "-pipeline-" + i);
            pipeline.setBusinessType("SERVICES");
            pipeline.setActive(true);
            pipeline = pipelineRepository.save(pipeline);
            for (int s = 0; s < STAGES_PER_PIPELINE; s++) {
                PipelineStage stage = new PipelineStage();
                stage.setPipeline(pipeline);
                stage.setName("etapa-" + s);
                stage.setDisplayOrder(STAGES_PER_PIPELINE - s);
                stage.setDefaultProbability(BigDecimal.ZERO);
                pipelineStageRepository.save(stage);
            }
        }
        return token;
    }

    private String seedRoles(int rows) {
        String token = uniqueToken();
        List<Permission> permissions = permissionRepository.findAll().subList(0, PERMISSIONS_PER_ROLE);
        for (int i = 0; i < rows; i++) {
            Role role = newRole(token + "-role-" + i);
            for (Permission permission : permissions) {
                RolePermission rolePermission = new RolePermission();
                rolePermission.setRole(role);
                rolePermission.setPermission(permission);
                rolePermissionRepository.save(rolePermission);
            }
        }
        return token;
    }

    private String seedUsers(int rows) {
        String token = uniqueToken();
        List<Role> roles = new ArrayList<>();
        for (int r = 0; r < ROLES_PER_USER; r++) {
            roles.add(newRole(token + "-user-role-" + r));
        }
        for (int i = 0; i < rows; i++) {
            User user = new User();
            user.setName(token + "-user-" + i);
            user.setEmail(token + "-" + i + "@querycount.local");
            user.setLogin(token + "-" + i);
            user.setPasswordHash("not-a-real-hash");
            user = userRepository.save(user);
            for (Role role : roles) {
                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(role);
                userRoleRepository.save(userRole);
            }
        }
        return token;
    }

    private Role newRole(String name) {
        Role role = new Role();
        role.setName(name);
        role.setActive(true);
        return roleRepository.save(role);
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
