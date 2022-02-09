/*******************************************************************************
 * Copyright (c) 2008, 2019 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.emf.mwe2.language.tests.validation;

import static org.eclipse.emf.mwe2.language.mwe2.Mwe2Package.Literals.*;
import static org.eclipse.emf.mwe2.language.validation.Mwe2Validator.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.mwe2.language.tests.Mwe2InjectorProvider;
import org.eclipse.emf.mwe2.language.mwe2.Module;
import org.eclipse.emf.mwe2.language.tests.factory.ComponentA;
import org.eclipse.emf.mwe2.language.tests.factory.ComponentAFactory;
import org.eclipse.emf.mwe2.language.tests.factory.SubTypeOfComponentA;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.eclipse.xtext.testing.util.ParseHelper;
import org.eclipse.xtext.testing.validation.ValidationTestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

@RunWith(XtextRunner.class)
@InjectWith(Mwe2InjectorProvider.class)
public class Mwe2ValidatorTest {

	@Inject
	private ParseHelper<Module> parser;

	private ValidationTestHelper validationTestHelper = new ValidationTestHelper(ValidationTestHelper.Mode.EXACT);

	@Test public void testAssignability() throws Exception {
		assertError("module foo " + ComponentA.class.getName() + " { y = true }", "true",
				ASSIGNMENT, INCOMPATIBLE_ASSIGNMENT,
				"A value of type 'boolean' can not be assigned to the feature org.eclipse.emf.mwe2.language.tests.factory.ComponentA.addY(java.lang.String)");
	}

	@Test public void testAssignability_2() throws Exception {
		assertError("module foo " + ComponentA.class.getName() + " { y = 'foo' x = 'bar' }", "'bar'",
				ASSIGNMENT, INCOMPATIBLE_ASSIGNMENT,
				"A value of type 'java.lang.String' can not be assigned to the feature org.eclipse.emf.mwe2.language.tests.factory.ComponentA.setX(org.eclipse.emf.mwe2.language.tests.factory.ComponentA)");
	}

	@Test public void testAssignability_3() throws Exception {
		assertNoIssues(
				"module foo " + ComponentA.class.getName() + " { x = " + SubTypeOfComponentA.class.getName() + "{} }");
	}

	@Test public void testAssignability_4() throws Exception {
		assertError(
				"module foo " + SubTypeOfComponentA.class.getName() + " { sub = " + ComponentA.class.getName() + "{} }",
				ComponentA.class.getName() + "{}", ASSIGNMENT, INCOMPATIBLE_ASSIGNMENT,
				"A value of type 'org.eclipse.emf.mwe2.language.tests.factory.ComponentA' can not be assigned to the feature org.eclipse.emf.mwe2.language.tests.factory.SubTypeOfComponentA.setSub(org.eclipse.emf.mwe2.language.tests.factory.SubTypeOfComponentA)");
	}

	@Test public void testAssignability_5() throws Exception {
		assertNoIssues("module foo " + ComponentA.class.getName() + " { b = true i = -1 d = -1.1 }");
	}

	@Test public void testAssignability_6() throws Exception {
		assertNoIssues("module foo " + ComponentA.class.getName() + " { d = 1 }");
	}

	@Test public void testAssignability_withFactory() throws Exception {
		assertNoIssues(
				"module foo " + ComponentA.class.getName() + " { x = " + ComponentAFactory.class.getName() + "{} }");
	}

	@Test public void testVarAssignability_withFactory() throws Exception {
		assertWarning(
				"module foo var " + ComponentA.class.getName() + " x = " + ComponentAFactory.class.getName() + "{} String {}",
				"x", DECLARED_PROPERTY, UNUSED_LOCAL,
				"The var 'x' is never read locally.");
	}

	@Test public void testVarAssignability_1() throws Exception {
		assertWarning("module foo var " + String.class.getName() + " x = 'x' String {}", "x",
				DECLARED_PROPERTY, UNUSED_LOCAL,
				"The var 'x' is never read locally.");
	}

	@Test public void testVarAssignability_2() throws Exception {
		String text = "module foo var " + String.class.getName() + " x = true String {}";
		assertWarning(text, "x", DECLARED_PROPERTY, UNUSED_LOCAL,
				"The var 'x' is never read locally.");
		assertError(text, "true", DECLARED_PROPERTY, INCOMPATIBLE_ASSIGNMENT,
				"A value of type 'boolean' can not be assigned to a reference of type java.lang.String");
	}

	@Test public void testVarAssignability_3() throws Exception {
		assertWarning("module foo var " + Boolean.class.getName() + " x = true String {}", "x",
				DECLARED_PROPERTY, UNUSED_LOCAL,
				"The var 'x' is never read locally.");
	}

	@Test public void testVarAssignability_4() throws Exception {
		String text = "module foo var " + Boolean.class.getName() + " x = 'foo' String {}";
		assertWarning(text, "x", DECLARED_PROPERTY, UNUSED_LOCAL,
				"The var 'x' is never read locally.");
		assertError(text, "'foo'", DECLARED_PROPERTY, INCOMPATIBLE_ASSIGNMENT,
				"A value of type 'java.lang.String' can not be assigned to a reference of type java.lang.Boolean");
	}

	@Test public void testUnusedLocalVariable() throws Exception {
		assertNoIssues("module foo var foo = 'holla' " + ComponentA.class.getName() + " : ups{ x = ups y = foo }");
	}

	@Test public void testUnusedLocalVariable_1() throws Exception {
		assertWarning("module m var foo = 'holla' " + ComponentA.class.getName() + " : ups{ x = ups }", "foo",
				DECLARED_PROPERTY, UNUSED_LOCAL,
				"The var 'foo' is never read locally.");
	}

	@Test public void testUnusedLocalVariable_2() throws Exception {
		assertWarning("module foo var foo = 'holla' " + ComponentA.class.getName() + " : ups { y = foo }", "ups",
				COMPONENT, UNUSED_LOCAL, "The var 'ups' is never read locally.");
	}

	@Test public void testUnusedLocalVariable_3() throws Exception {
		assertNoIssues("module foo var foo = 'holla' var bar = '${foo}!' " + ComponentA.class.getName() + "{ y = bar}");
	}

	@Test public void testUnusedLocalVariable_4() throws Exception {
		assertNoIssues("module foo var y = 'holla' " + ComponentA.class.getName() + " auto-inject { }");
	}

	@Test public void testUnusedLocalVariable_5() throws Exception {
		assertWarning("module foo var y = 'holla' " + ComponentA.class.getName() + " auto-inject { y = 'zonk' }", "y",
				DECLARED_PROPERTY, UNUSED_LOCAL,
				"The var 'y' is never read locally.");
	}

	@Test public void testUnusedLocalVariable_6() throws Exception {
		assertNoIssues("module foo var y = 'holla' " + ComponentA.class.getName() + " auto-inject { y = y }");
	}

	@Test public void testUnusedLocalVariable_7() throws Exception {
		assertNoIssues("module foo var y = 'holla' " + ComponentA.class.getName() + " auto-inject { y = '${y}' }");
	}

	@Test public void testUnusedLocalVariable_8() throws Exception {
		String fooText = "module foo var foo.bar = 'holla' @bar auto-inject {}";
		String barText = "module bar var foo.bar = '' " + ComponentA.class.getName() + " { y = foo.bar }";
		Module fooModule = parser.parse(fooText);
		Module barModule = parser.parse(barText, fooModule.eResource().getResourceSet());
			
		validationTestHelper.assertNoIssues(fooModule);
		validationTestHelper.assertNoIssues(barModule);
	}

	@Test public void testDuplicateLocalVariable_1() throws Exception {
		String text = "module m var foo = 'holla' var foo = '${foo}!' " + ComponentA.class.getName() + "{ y = foo}";
		assertError(text, "foo", DECLARED_PROPERTY, DUPLICATE_LOCAL, "Duplicate var 'foo'.");
		assertError(text, 31, 3, DECLARED_PROPERTY, DUPLICATE_LOCAL, "Duplicate var 'foo'.");
	}

	@Test public void testMandatoryProperty_1() throws Exception {
		String fooText = "module foo @bar { foo = 'zonk' }";
		String barText = "module bar var foo.bar var foo " + ComponentA.class.getName() + " { y = foo.bar }";
		
		Module fooModule = parser.parse(fooText);
		Module barModule = parser.parse(barText, fooModule.eResource().getResourceSet());
			
		validationTestHelper.assertError(fooModule, COMPONENT, MISSING_MANDATORY_FEATURE, "Mandatory feature was not assigned: 'foo.bar'.");
		validationTestHelper.assertWarning(barModule, DECLARED_PROPERTY, UNUSED_LOCAL, "The var 'foo' is never read locally.");
	}

	@Test public void testDeprecatedElement() throws Exception {
		assertWarning(
				"module m\r\n" + 
				"\r\n" + 
				"import org.eclipse.xtext.xtext.generator.XtextGenerator\r\n" + 
				"import org.eclipse.xtext.xtext.generator.StandardLanguage\r\n" + 
				"\r\n" + 
				"Workflow {\r\n" + 
				"	component = XtextGenerator {\r\n" + 
				"		language = StandardLanguage {\r\n" + 
				"			newProjectWizardForEclipse = {}\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}", "newProjectWizardForEclipse", ASSIGNMENT, DEPRECATED_ELEMENT,
				"The 'org.eclipse.xtext.xtext.generator.StandardLanguage.setNewProjectWizardForEclipse' is deprecated.");
	}

	private void assertWarning(String text, String errorProneText, EClass objectType, String code, String message) throws Exception {
		validationTestHelper.assertWarning(parse(text), objectType, code, text.indexOf(errorProneText),
				errorProneText.length(), message);
	}

	private void assertError(String text, String errorProneText, EClass objectType, String code, String message) throws Exception {
		validationTestHelper.assertError(parse(text), objectType, code, text.indexOf(errorProneText),
				errorProneText.length(), message);
	}

	private void assertError(String text, int errorProneTextOffset, int errorProneTextLength, EClass objectType,
			String code, String message) throws Exception {
		validationTestHelper.assertError(parse(text), objectType, code, errorProneTextOffset, errorProneTextLength, message);
	}

	private void assertNoIssues(String text) throws Exception {
		validationTestHelper.assertNoIssues(parse(text));
	}

	private Module parse(String text) throws Exception {
		return parser.parse(text);
	}
}
