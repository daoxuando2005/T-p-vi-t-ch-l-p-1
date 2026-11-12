package com.hocchu.web;

import com.hocchu.web.entity.User;
import com.hocchu.web.security.*;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import jakarta.servlet.FilterChain;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionVersionTest {
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    @Test void enabledAccountAndMatchingSessionVersionAreBothRequired() throws Exception {
        for(boolean enabled:new boolean[]{false,true})for(int version:new int[]{0,1}) {
            SecurityContextHolder.clearContext();var filter=new JwtAuthenticationFilter();var provider=mock(JwtTokenProvider.class);var details=mock(CustomUserDetailsService.class);
            var principal=UserPrincipal.create(User.builder().id(9L).username("fixture").enabled(enabled).sessionVersion(version).build());
            ReflectionTestUtils.setField(filter,"tokenProvider",provider);ReflectionTestUtils.setField(filter,"customUserDetailsService",details);
            when(provider.validateToken("fixture.jwt.token")).thenReturn(true);when(provider.getUserIdFromJWT("fixture.jwt.token")).thenReturn(9L);
            when(details.loadUserById(9L)).thenReturn(principal);
            var request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer fixture.jwt.token");var chain=mock(FilterChain.class);
            filter.doFilter(request,new MockHttpServletResponse(),chain);verify(chain,times(1)).doFilter(any(),any());
            assertEquals(enabled&&version==0,SecurityContextHolder.getContext().getAuthentication()!=null);
        }
    }
    @Test void newlyIssuedTokenContainsCurrentAccountVersion() {
        var provider=new JwtTokenProvider();ReflectionTestUtils.setField(provider,"jwtSecret","fixture-only-secret-".repeat(5));ReflectionTestUtils.setField(provider,"jwtExpirationInMs",60000L);
        var principal=UserPrincipal.create(User.builder().id(9L).username("fixture").enabled(true).sessionVersion(4).build());
        var token=provider.generateToken(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
        assertTrue(provider.validateToken(token));assertEquals(4,provider.getSessionVersion(token));
    }
    @Test void earlierTokensWithoutVersionStillUseVersionZero() {
        String secret="fixture-only-secret-".repeat(5);var provider=new JwtTokenProvider();ReflectionTestUtils.setField(provider,"jwtSecret",secret);
        String legacy=io.jsonwebtoken.Jwts.builder().setSubject("9").setExpiration(new java.util.Date(System.currentTimeMillis()+60000))
            .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8))).compact();
        assertTrue(provider.validateToken(legacy));assertEquals(0,provider.getSessionVersion(legacy));
    }
}
