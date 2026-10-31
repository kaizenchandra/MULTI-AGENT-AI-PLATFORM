package com.kaizen.platform;

import static org.junit.jupiter.api.Assertions.*;
import java.security.*;
import java.security.interfaces.*;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;

class JwtSecurityTest {
 String token(KeyPair key,String issuer,String audience,Instant expiry) throws Exception {
  var claims=new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject("user-a").issueTime(Date.from(Instant.now().minusSeconds(30))).expirationTime(Date.from(expiry)).claim("tenant_id","tenant-a").claim("scope","knowledge:read").build();
  var jwt=new SignedJWT(new JWSHeader(JWSAlgorithm.RS256),claims);jwt.sign(new RSASSASigner((RSAPrivateKey)key.getPrivate()));return jwt.serialize();
 }
 @Test void actualSignatureIssuerAudienceAndExpirationValidation() throws Exception {
  KeyPair key=KeyPairGenerator.getInstance("RSA").generateKeyPair();
  var decoder=NimbusJwtDecoder.withPublicKey((RSAPublicKey)key.getPublic()).build();decoder.setJwtValidator(SecurityConfig.validators("https://identity.example","agents-api"));
  assertEquals("tenant-a",decoder.decode(token(key,"https://identity.example","agents-api",Instant.now().plusSeconds(300))).getClaimAsString("tenant_id"));
  assertThrows(JwtException.class,()->decoder.decode(token(key,"https://evil.example","agents-api",Instant.now().plusSeconds(300))));
  assertThrows(JwtException.class,()->decoder.decode(token(key,"https://identity.example","wrong",Instant.now().plusSeconds(300))));
  assertThrows(JwtException.class,()->decoder.decode(token(key,"https://identity.example","agents-api",Instant.now().minusSeconds(300))));
  KeyPair attacker=KeyPairGenerator.getInstance("RSA").generateKeyPair();assertThrows(JwtException.class,()->decoder.decode(token(attacker,"https://identity.example","agents-api",Instant.now().plusSeconds(300))));
 }
}
