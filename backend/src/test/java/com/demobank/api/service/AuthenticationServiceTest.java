package com.demobank.api.service;

import com.demobank.api.dto.AuthenticationRequest;
import com.demobank.api.dto.AuthenticationResponse;
import com.demobank.api.dto.RegisterRequest;
import com.demobank.api.model.Role;
import com.demobank.api.model.User;
import com.demobank.api.repository.UserRepository;
import com.demobank.api.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Test
    void register_savesUserWithEncodedPasswordAndReturnsToken() {
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.register(
                new RegisterRequest("Alice", "Martin", "alice@demobank.test", "secret"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertThat(user.getEmail()).isEqualTo("alice@demobank.test");
        assertThat(user.getPassword()).isEqualTo("hashed");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void authenticate_checksCredentialsAndReturnsToken() {
        User user = User.builder().email("alice@demobank.test").password("hashed").role(Role.ROLE_USER).build();
        when(userRepository.findByEmail("alice@demobank.test")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthenticationResponse response = authenticationService.authenticate(
                new AuthenticationRequest("alice@demobank.test", "secret"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> auth =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(auth.capture());
        assertThat(auth.getValue().getPrincipal()).isEqualTo("alice@demobank.test");
        assertThat(auth.getValue().getCredentials()).isEqualTo("secret");
        assertThat(response.getToken()).isEqualTo("jwt-token");
    }

    @Test
    void authenticate_doesNotIssueTokenOnBadCredentials() {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authenticationService.authenticate(
                new AuthenticationRequest("alice@demobank.test", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generateToken(any());
    }
}
