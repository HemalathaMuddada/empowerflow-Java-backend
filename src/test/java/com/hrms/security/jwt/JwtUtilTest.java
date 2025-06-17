package com.hrms.security.jwt;

import com.hrms.security.service.UserDetailsImpl;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class) // Not strictly necessary if not using @Mock, but good practice
class JwtUtilTest {

    @InjectMocks // Allows ReflectionTestUtils to work on an instance
    private JwtUtil jwtUtil;

    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        // Initialize JwtUtil with test properties
        // Secret must be long enough for HS256, at least 32 bytes (256 bits)
        String testSecret = "TestSecretKeyForJwtUtilUnitTestsPleaseMakeItLongEnough";
        ReflectionTestUtils.setField(jwtUtil, "jwtSecret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", (int) TimeUnit.HOURS.toMillis(1)); // 1 hour

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        userDetails = new UserDetailsImpl(1L, "testuser", "test@example.com", "password", 100L, authorities);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        String token = jwtUtil.generateToken(userDetails);
        assertThat(token).isNotNull().isNotEmpty();

        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("testuser");
        assertThat(claims.get("userId", Long.class)).isEqualTo(1L);
        assertThat(claims.get("companyId", Long.class)).isEqualTo(100L);
        assertThat(claims.get("roles", List.class)).contains("ROLE_USER");
    }

    @Test
    void generateToken_withUsernameRolesCompanyId_shouldReturnValidToken() {
        String token = jwtUtil.generateToken("anotheruser", List.of("ROLE_ADMIN"), 200L);
        assertThat(token).isNotNull().isNotEmpty();

        Claims claims = jwtUtil.extractAllClaims(token);
        assertThat(claims.getSubject()).isEqualTo("anotheruser");
        assertThat(claims.get("companyId", Long.class)).isEqualTo(200L);
        assertThat(claims.get("roles", List.class)).contains("ROLE_ADMIN");
        // userId is not included by this overloaded method currently
        assertThat(claims.get("userId")).isNull();
    }


    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken(userDetails);
        String username = jwtUtil.extractUsername(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void validateToken_withValidTokenAndUserDetails_shouldReturnTrue() {
        String token = jwtUtil.generateToken(userDetails);
        Boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_withValidToken_shouldReturnTrue() {
        String token = jwtUtil.generateToken(userDetails);
        Boolean isValid = jwtUtil.validateToken(token);
        assertThat(isValid).isTrue();
    }

    @Test
    void validateToken_withExpiredToken_shouldReturnFalse() throws InterruptedException {
        // Override expiration to a very short time for testing
        ReflectionTestUtils.setField(jwtUtil, "jwtExpirationMs", 1); // 1 millisecond
        String token = jwtUtil.generateToken(userDetails);

        // Wait for token to expire
        Thread.sleep(10); // Sleep for 10ms to ensure expiration

        Boolean isValid = jwtUtil.validateToken(token, userDetails);
        assertThat(isValid).isFalse();

        Boolean isValidBasic = jwtUtil.validateToken(token);
        assertThat(isValidBasic).isFalse(); // Basic validation should also catch expiration
    }

    @Test
    void validateToken_withMalformedToken_shouldReturnFalse() {
        String malformedToken = "this.is.not.a.jwt";
         // The Jwts parser throws MalformedJwtException if token structure is wrong
        // The validateToken(String) method catches this and returns false.
        assertThat(jwtUtil.validateToken(malformedToken)).isFalse();

        // validateToken(String, UserDetails) would first try to extract username, which would fail.
        assertThrows(MalformedJwtException.class, () -> {
            jwtUtil.validateToken(malformedToken, userDetails);
        });
    }

    @Test
    void validateToken_withDifferentUsernameInToken_shouldReturnFalse() {
        String token = jwtUtil.generateToken(userDetails); // Token for "testuser"
        List<GrantedAuthority> authoritiesOther = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        UserDetails otherUserDetails = new UserDetailsImpl(2L, "otheruser", "other@example.com", "password", 101L, authoritiesOther);

        Boolean isValid = jwtUtil.validateToken(token, otherUserDetails);
        assertThat(isValid).isFalse();
    }

    @Test
    void extractExpiration_shouldReturnValidDate() {
        String token = jwtUtil.generateToken(userDetails);
        Date expiration = jwtUtil.extractExpiration(token);
        assertThat(expiration).isNotNull();
        assertThat(expiration.after(new Date())).isTrue(); // Should be in the future
    }
}
