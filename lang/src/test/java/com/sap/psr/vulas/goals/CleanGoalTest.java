package com.sap.psr.vulas.goals;

import static com.jayway.restassured.RestAssured.expect;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.charset;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Action.stringContent;
import static com.xebialabs.restito.semantics.Condition.composite;
import static com.xebialabs.restito.semantics.Condition.method;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.uri;

import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.Test;

import com.sap.psr.vulas.core.util.CoreConfiguration;
import com.sap.psr.vulas.shared.connectivity.PathBuilder;
import com.sap.psr.vulas.shared.enums.GoalClient;
import com.sap.psr.vulas.shared.enums.GoalType;
import com.sap.psr.vulas.shared.json.JacksonUtil;
import com.sap.psr.vulas.shared.json.model.Application;
import com.sap.psr.vulas.shared.util.VulasConfiguration;

public class CleanGoalTest extends AbstractGoalTest {
	
	/**
	 * Same as in {@link BomGoalTest}.
	 */
	private void setupMockServices(Application _a) {
		final String s_json = JacksonUtil.asJsonString(_a);
		
		// Create app
		whenHttp(server).
		match(post("/backend" + PathBuilder.apps())).
		then(
				stringContent(s_json),
				contentType("application/json"),
				charset("UTF-8"),
				status(HttpStatus.CREATED_201));
		
		expect()
			.statusCode(201).
			when()
			.post("/backend" + PathBuilder.apps());

		// Create goal exe
		whenHttp(server).
		match(post("/backend" + PathBuilder.goalExcecutions(null, null, _a))).
		then(
				stringContent(s_json),
				contentType("application/json"),
				charset("UTF-8"),
				status(HttpStatus.CREATED_201));
		
		expect()
			.statusCode(201).
			when()
			.post("/backend" + PathBuilder.goalExcecutions(null, null, _a));

		// Options app
		whenHttp(server).
				match(composite(method(Method.OPTIONS), uri("/backend" + PathBuilder.goalExcecutions(null, null, _a)))).
			then(
				stringContent(s_json),
				contentType("application/json"),
				charset("UTF-8"),
				status(HttpStatus.OK_200));
	}
	
	/**
	 * Create empty app, clean and upload again
	 */
	@Test
	public void testClean() throws GoalConfigurationException, GoalExecutionException {
		// Mock REST services
		this.configureBackendServiceUrl(server);
		this.setupMockServices(this.testApp);

		// Set config
		final VulasConfiguration cfg = new VulasConfiguration();
		cfg.setProperty(CoreConfiguration.TENANT_TOKEN, "foo");
		cfg.setProperty(CoreConfiguration.APP_CTX_GROUP, this.testApp.getMvnGroup());
		cfg.setProperty(CoreConfiguration.APP_CTX_ARTIF, this.testApp.getArtifact());
		cfg.setProperty(CoreConfiguration.APP_CTX_VERSI, this.testApp.getVersion());
		cfg.setProperty(CoreConfiguration.APP_UPLOAD_EMPTY, new Boolean(true));
				
		// APP
		AbstractGoal goal = GoalFactory.create(GoalType.APP, GoalClient.CLI);
		goal.setConfiguration(cfg).executeSync();
		
		// CLEAN
		goal = GoalFactory.create(GoalType.CLEAN, GoalClient.CLI);
		goal.setConfiguration(cfg).executeSync();
		
		// Check the HTTP calls made
		verifyHttp(server).times(2, 
				method(Method.POST),
				uri("/backend" + PathBuilder.apps()));
		verifyHttp(server).times(2, 
				method(Method.POST),
				uri("/backend" + PathBuilder.goalExcecutions(null, null, this.testApp)));
	}
}
