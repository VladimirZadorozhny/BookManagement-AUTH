package org.mystudying.bookmanagementauth.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mystudying.bookmanagementauth.dto.RegisterRequestDto;
import org.mystudying.bookmanagementauth.dto.UserDto;
import org.mystudying.bookmanagementauth.services.UserAuthLifecycleService;
import org.mystudying.bookmanagementauth.services.UserBookingService;
import org.mystudying.bookmanagementauth.services.UserService;
import org.mystudying.bookmanagementauth.services.VerificationService;
import org.mystudying.bookmanagementauth.support.concurrency.ConcurrentTestHelperBarrier;
import org.mystudying.bookmanagementauth.support.db.TestFixtures;
import org.mystudying.bookmanagementauth.support.db.TestDataCleanup;
import org.mystudying.bookmanagementauth.support.db.TestDataHelper;
import org.mystudying.bookmanagementauth.support.mail.MailTestUtils;
import org.mystudying.bookmanagementauth.support.concurrency.ConcurrentTestHelperLatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
public abstract class AbstractSecurityIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TestDataHelper testDataHelper;

    @Autowired
    protected TestDataCleanup testDataCleanup;

    @Autowired
    protected MailTestUtils mailTestUtils;

    @Autowired
    protected ConcurrentTestHelperLatch concurrentTestHelperLatch;

    @Autowired
    protected ConcurrentTestHelperBarrier concurrentTestHelperBarrier;

    @Autowired
    protected UserService userService;

    @Autowired
    protected UserAuthLifecycleService authLifecycleService;

    @Autowired
    protected UserBookingService userBookingService;

    @Autowired
    protected VerificationService verificationService;

    @Autowired
    protected JdbcClient jdbcClient;

    protected MockHttpSession loginAs(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                        .param("username", username)
                        .param("password", password)
        ).andExpect(status().isOk()).andReturn();

        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected MockHttpSession loginAsAdmin() throws Exception {
        return loginAs("admin@library.com", "admin");
    }

    protected MockHttpSession loginAsUser() throws Exception {
        return loginAs(TestFixtures.USER_1_EMAIL, TestFixtures.COMMON_PASSWORD);
    }

    /**
     * Helper to register a new user and verify them immediately.
     */
    protected UserDto signupAndVerify(String name, String email, String password) {
        RegisterRequestDto registration = new RegisterRequestDto(name, email, password);
        UserDto userDto = authLifecycleService.register(registration);

        String token = jdbcClient.sql("SELECT token FROM verification_token WHERE user_id = ?")
                .param(userDto.id()).query(String.class).single();

        verificationService.verifyToken(token);
        return userDto;
    }
}
