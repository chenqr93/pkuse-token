/*
 * Copyright 2020-2099 sa-token.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bdsoft.pkusetoken.plugin;

import com.bdsoft.pkusetoken.oauth2.PkuseOAuth2Manager;
import com.bdsoft.pkusetoken.oauth2.config.PkuseOAuth2ServerConfig;
import com.bdsoft.pkusetoken.oauth2.data.model.loader.PkuseClientModel;
import com.bdsoft.pkusetoken.oauth2.exception.PkuseOAuth2Exception;
import com.bdsoft.pkusetoken.oauth2.template.PkuseOAuth2Template;
import com.bdsoft.pkusetoken.sso.PkuseSsoManager;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoClientModel;
import com.bdsoft.pkusetoken.sso.config.PkuseSsoServerConfig;
import com.bdsoft.pkusetoken.sso.exception.PkuseSsoException;
import com.bdsoft.pkusetoken.sso.template.PkuseSsoServerTemplate;
import com.bdsoft.pkusetoken.util.PkuseFoxUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * SSO / OAuth2 allow-url 通配符配置校验
 */
public class AllowUrlWildcardPatternTest {

	private PkuseSsoServerConfig backupSsoServerConfig;
	private PkuseOAuth2ServerConfig backupOAuth2ServerConfig;

	@BeforeEach
	public void backupManagers() {
		backupSsoServerConfig = PkuseSsoManager.getServerConfig();
		backupOAuth2ServerConfig = PkuseOAuth2Manager.getServerConfig();
	}

	@AfterEach
	public void restoreManagers() {
		PkuseSsoManager.setServerConfig(backupSsoServerConfig);
		PkuseOAuth2Manager.setServerConfig(backupOAuth2ServerConfig);
	}

	@Test
	public void oauth2AllowUrlWildcardPattern() {
		Assertions.assertDoesNotThrow(() ->
				PkuseOAuth2Template.checkRedirectUriListNormalStaticMethod(
						Arrays.asList("http://sa-sso-client1.com/sso/login", "http://sa-sso-client1.com/*", "http://sa-sso-client1.com:9003/*", "http://sa-sso-client1.com:*")));
		Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				PkuseOAuth2Template.checkRedirectUriListNormalStaticMethod(
						Collections.singletonList("http://sa-sso-client1.com*")));
		Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				PkuseOAuth2Template.checkRedirectUriListNormalStaticMethod(
						Collections.singletonList("http://sa-sso-client1.com:9003*")));
		Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				PkuseOAuth2Template.checkRedirectUriListNormalStaticMethod(
						Collections.singletonList("http://*.sa-sso-client1.com/")));
	}

	@Test
	public void ssoAllowUrlWildcardPattern() {
		Assertions.assertDoesNotThrow(() ->
				PkuseSsoServerTemplate.checkAllowUrlListStaticMethod(
						Arrays.asList("*", "http://sa-sso-client1.com:9003/*", "http://sa-sso-client1.com:*")));
		Assertions.assertThrows(PkuseSsoException.class, () ->
				PkuseSsoServerTemplate.checkAllowUrlListStaticMethod(
						Collections.singletonList("http://sa-sso-client1.com*")));
		Assertions.assertThrows(PkuseSsoException.class, () ->
				PkuseSsoServerTemplate.checkAllowUrlListStaticMethod(
						Collections.singletonList("http://sa-sso-client1.com:9003*")));
	}

	@Test
	public void oauth2RejectAtInRedirectUri() {
		PkuseOAuth2Template template = new PkuseOAuth2Template();
		Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				template.checkRedirectUri("1001", "http://sa-oauth-client.com:123@sa-token.com"));
		Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				template.checkRedirectUri("1001", "http://sa-oauth-client.com:123%40sa-token.com"));
		Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				template.checkRedirectUri("1001", "http://sa-oauth-client.com:123%2540sa-token.com"));
	}

	@Test
	public void ssoRejectAtInRedirectUrl() {
		PkuseSsoServerTemplate template = new PkuseSsoServerTemplate();
		Assertions.assertThrows(PkuseSsoException.class, () ->
				template.checkRedirectUrl("sso-client1", "http://sa-sso-client1.com:9003@sa-token.com"));
		Assertions.assertThrows(PkuseSsoException.class, () ->
				template.checkRedirectUrl("sso-client1", "http://sa-sso-client1.com:9003%40sa-token.com"));
		Assertions.assertThrows(PkuseSsoException.class, () ->
				template.checkRedirectUrl("sso-client1", "http://sa-sso-client1.com:9003%2540sa-token.com"));
	}

	@Test
	public void redirectUrlIpv6AllowUrlConfig() {
		Assertions.assertDoesNotThrow(() ->
				PkuseSsoServerTemplate.checkAllowUrlListStaticMethod(
						Arrays.asList("http://[::1]:9003/*", "http://[2001:db8::1]:8080/*")));
		Assertions.assertDoesNotThrow(() ->
				PkuseOAuth2Template.checkRedirectUriListNormalStaticMethod(
						Collections.singletonList("http://[::1]:9003/*")));
		Assertions.assertTrue(PkuseFoxUtil.vagueMatch("http://[::1]:9003/*", "http://[::1]:9003/sso/login"));
		Assertions.assertTrue(PkuseFoxUtil.vagueMatch("http://[2001:db8::1]:8080/*", "http://[2001:db8::1]:8080/callback"));
	}

	@Test
	public void ssoCheckRedirectUrl_ipv6_success() {
		PkuseSsoClientModel client = new PkuseSsoClientModel()
				.setClient("ipv6-client")
				.setAllowUrl("http://[::1]:9003/*,http://[2001:db8::1]:8080/*");
		Map<String, PkuseSsoClientModel> clients = new HashMap<>();
		clients.put("ipv6-client", client);
		PkuseSsoManager.setServerConfig(new PkuseSsoServerConfig().setClients(clients));

		PkuseSsoServerTemplate template = new PkuseSsoServerTemplate();
		Assertions.assertDoesNotThrow(() ->
				template.checkRedirectUrl("ipv6-client", "http://[::1]:9003/sso/login"));
		Assertions.assertDoesNotThrow(() ->
				template.checkRedirectUrl("ipv6-client", "http://[2001:db8::1]:8080/callback?back=/"));
	}

	@Test
	public void ssoCheckRedirectUrl_ipv6_rejectUnbracketedAndIllegal() {
		PkuseSsoClientModel client = new PkuseSsoClientModel()
				.setClient("ipv6-client")
				.setAllowUrl("http://[::1]:9003/*");
		Map<String, PkuseSsoClientModel> clients = new HashMap<>();
		clients.put("ipv6-client", client);
		PkuseSsoManager.setServerConfig(new PkuseSsoServerConfig().setClients(clients));

		PkuseSsoServerTemplate template = new PkuseSsoServerTemplate();
		PkuseSsoException ex1 = Assertions.assertThrows(PkuseSsoException.class, () ->
				template.checkRedirectUrl("ipv6-client", "http://::1:9003/sso/login"));
		Assertions.assertTrue(ex1.getMessage().contains("无效redirect"));

		PkuseSsoException ex2 = Assertions.assertThrows(PkuseSsoException.class, () ->
				template.checkRedirectUrl("ipv6-client", "http://[::2]:9003/sso/login"));
		Assertions.assertTrue(ex2.getMessage().contains("非法redirect"));

		Assertions.assertThrows(PkuseSsoException.class, () ->
				template.checkRedirectUrl("ipv6-client", "http://[::1]:9003@evil.com/sso/login"));
	}

	@Test
	public void oauth2CheckRedirectUri_ipv6_success() {
		PkuseClientModel client = new PkuseClientModel()
				.setClientId("ipv6-oauth2")
				.addAllowRedirectUris("http://[::1]:9003/*", "http://[2001:db8::1]:8080/callback");
		Map<String, PkuseClientModel> clients = new HashMap<>();
		clients.put("ipv6-oauth2", client);
		PkuseOAuth2Manager.setServerConfig(new PkuseOAuth2ServerConfig().setClients(clients));

		PkuseOAuth2Template template = new PkuseOAuth2Template();
		Assertions.assertDoesNotThrow(() ->
				template.checkRedirectUri("ipv6-oauth2", "http://[::1]:9003/sso/login"));
		Assertions.assertDoesNotThrow(() ->
				template.checkRedirectUri("ipv6-oauth2", "http://[2001:db8::1]:8080/callback"));
	}

	@Test
	public void oauth2CheckRedirectUri_ipv6_rejectUnbracketedAndIllegal() {
		PkuseClientModel client = new PkuseClientModel()
				.setClientId("ipv6-oauth2")
				.addAllowRedirectUris("http://[::1]:9003/*");
		Map<String, PkuseClientModel> clients = new HashMap<>();
		clients.put("ipv6-oauth2", client);
		PkuseOAuth2Manager.setServerConfig(new PkuseOAuth2ServerConfig().setClients(clients));

		PkuseOAuth2Template template = new PkuseOAuth2Template();
		PkuseOAuth2Exception ex1 = Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				template.checkRedirectUri("ipv6-oauth2", "http://2001:db8::1:8080/callback"));
		Assertions.assertTrue(ex1.getMessage().contains("无效 redirect_url"));

		PkuseOAuth2Exception ex2 = Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				template.checkRedirectUri("ipv6-oauth2", "http://[::2]:9003/sso/login"));
		Assertions.assertTrue(ex2.getMessage().contains("非法 redirect_url"));

		Assertions.assertThrows(PkuseOAuth2Exception.class, () ->
				template.checkRedirectUri("ipv6-oauth2", "http://[::1]:9003%40evil.com/sso/login"));
	}

}
