package com.peopledesk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peopledesk.department.Department;
import com.peopledesk.department.DepartmentRepository;
import com.peopledesk.user.Role;
import com.peopledesk.user.User;
import com.peopledesk.user.UserRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Full stack (security → controller → service → JPA → H2 with the real Flyway schema). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private UserRepository users;
    @Autowired
    private DepartmentRepository departments;
    @Autowired
    private PasswordEncoder encoder;

    private Long departmentId;

    @BeforeEach
    void seed() {
        if (users.count() == 0) {
            users.save(new User("admin", encoder.encode("Admin@123"), "Alex Morgan", Role.ADMIN));
            users.save(new User("viewer", encoder.encode("Viewer@123"), "Riley Brooks", Role.USER));
            departments.save(new Department("Engineering"));
        }
        departmentId = departments.findAll().getFirst().getId();
    }

    // ---------- authentication ----------

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "admin", "password", "nope-nope"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Username or password is incorrect."));
    }

    @Test
    void loginValidatesFieldLengths() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", "ab", "password", "123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void protectedEndpointsRequireToken() throws Exception {
        mvc.perform(get("/api/employees")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesAndLogoutRevokes() throws Exception {
        JsonNode login = login("admin", "Admin@123");
        String access = login.get("accessToken").asText();
        String refresh = login.get("refreshToken").asText();

        JsonNode refreshed = json.readTree(mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refresh))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        // The old refresh token is single-use.
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refresh))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refreshed.get("refreshToken").asText()))))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("refreshToken", refreshed.get("refreshToken").asText()))))
                .andExpect(status().isUnauthorized());
    }

    // ---------- employees CRUD ----------

    @Test
    void adminCanCreateReadUpdateAndDelete() throws Exception {
        String token = token("admin", "Admin@123");
        String email = "crud-" + UUID.randomUUID() + "@Example.com";

        String created = mvc.perform(post("/api/employees").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(employee("Grace Hopper", email)))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, containsString("/api/employees/")))
                .andExpect(jsonPath("$.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.departmentName").value("Engineering"))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(created).get("id").asLong();

        mvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, token)
                        .param("search", "grace hop").param("sort", "joined").param("direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content[*].id", hasItem((int) id)));

        mvc.perform(put("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(employee("Grace B. Hopper", email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Grace B. Hopper"));

        mvc.perform(delete("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/employees/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
    }

    @Test
    void duplicateEmailIsConflictOnEmailField() throws Exception {
        String token = token("admin", "Admin@123");
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/employees").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(employee("First Person", email)))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/employees").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(employee("Second Person", email.toUpperCase())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").value("An employee with this email already exists."));
    }

    @Test
    void invalidEmployeeReturnsFieldErrors() throws Exception {
        String token = token("admin", "Admin@123");
        String invalid = body(Map.of("name", "", "email", "not-an-email", "phone", "abc", "title", "",
                "departmentId", departmentId, "status", "ACTIVE", "joinedOn", LocalDate.now().plusDays(3).toString()));

        mvc.perform(post("/api/employees").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.phone").exists())
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.fieldErrors.joinedOn").exists());
    }

    @Test
    void searchTreatsSqlAndWildcardsAsPlainText() throws Exception {
        String token = token("admin", "Admin@123");
        mvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, token).param("search", "' OR 1=1 --"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, token).param("search", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void viewerIsReadOnlyAndHasNoReports() throws Exception {
        String token = token("viewer", "Viewer@123");

        mvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(get("/api/employees/stats").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isOk());
        mvc.perform(post("/api/employees").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(employee("Nope", "nope@example.com")))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/employees/1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/reports/summary").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    // ---------- reports ----------

    @Test
    void reportSummaryRowsAndExports() throws Exception {
        String token = token("admin", "Admin@123");
        String email = "report-" + UUID.randomUUID() + "@example.com";
        mvc.perform(post("/api/employees").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(employee("Report Person", email)))
                .andExpect(status().isCreated());
        String today = LocalDate.now().toString();

        mvc.perform(get("/api/reports/summary").header(HttpHeaders.AUTHORIZATION, token)
                        .param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.byStatus[?(@.key == 'ACTIVE')]").exists())
                .andExpect(jsonPath("$.byDepartment[0].key").value("Engineering"))
                .andExpect(jsonPath("$.byJoinMonth[0].label").exists());

        mvc.perform(get("/api/reports/employees").header(HttpHeaders.AUTHORIZATION, token)
                        .param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].email", hasItem(email)));

        mvc.perform(get("/api/reports/summary").header(HttpHeaders.AUTHORIZATION, token)
                        .param("from", today).param("to", "2000-01-01"))
                .andExpect(status().isBadRequest());

        byte[] xlsx = mvc.perform(get("/api/reports/export/excel").header(HttpHeaders.AUTHORIZATION, token)
                        .param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".xlsx")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(xlsx).startsWith('P', 'K'); // xlsx is a zip archive

        byte[] pdf = mvc.perform(get("/api/reports/export/pdf").header(HttpHeaders.AUTHORIZATION, token)
                        .param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    // ---------- helpers ----------

    private JsonNode login(String username, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(body(Map.of("username", username, "password", password, "rememberMe", false))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response);
    }

    private String token(String username, String password) throws Exception {
        return "Bearer " + login(username, password).get("accessToken").asText();
    }

    private String employee(String name, String email) throws Exception {
        return body(Map.of("name", name, "email", email, "phone", "+1 (202) 555-0100", "title", "Engineer",
                "departmentId", departmentId, "status", "ACTIVE", "joinedOn", LocalDate.now().toString()));
    }

    private String body(Object value) throws Exception {
        return json.writeValueAsString(value);
    }
}
