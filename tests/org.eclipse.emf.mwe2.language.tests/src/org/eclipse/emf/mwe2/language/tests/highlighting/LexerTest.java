/*******************************************************************************
 * Copyright (c) 2008,2010 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.emf.mwe2.language.tests.highlighting;

import static org.junit.Assert.*;

import java.util.List;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.Token;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.mwe2.language.ui.tests.Mwe2UiInjectorProvider;
import org.eclipse.emf.mwe2.language.services.Mwe2GrammarAccess;
import org.eclipse.emf.mwe2.language.ui.highlighting.MweHighlightingLexer;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.TerminalRule;
import org.eclipse.xtext.parser.antlr.ITokenDefProvider;
import org.eclipse.xtext.parser.antlr.XtextTokenStream;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.XtextRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

@InjectWith(Mwe2UiInjectorProvider.class)
@RunWith(XtextRunner.class)
public class LexerTest {

	@Inject
	private MweHighlightingLexer lexer;
	@Inject
	private ITokenDefProvider tokenDefProvider;
	@Inject
	private Mwe2GrammarAccess grammarAccess;
	
	@Test public void testEmptyLiteral() {
		parseStringLiteral("");
	}
	
	@Test public void testKeywords() {
		TreeIterator<EObject> iterator = EcoreUtil.getAllContents(grammarAccess.getGrammar(), false);
		while(iterator.hasNext()) {
			EObject next = iterator.next();
			if (next instanceof TerminalRule 
					|| next == grammarAccess.getConstantValueRule()) {
				iterator.prune();
			} else if (next instanceof Keyword) {
				String value = ((Keyword) next).getValue();
				if ("'".equals(value) || "\"".equals(value) || "\\".equals(value) || "${".equals(value))
					value = "\\" + value;
				parseStringLiteral(value);
			}
		}
	}
	
	@Test public void testKeywordPrefixes() {
		parseStringLiteral("$",	"impo",	"/");
	}
	
	@Test public void testComments() {
		parseStringLiteral("// something\n", "/* something */");
	}
	
	@Test public void testEscapedComments() {
		parseStringLiteral("\\// something", "\\/* something");
	}
	
	@Test public void testWS() {
		parseStringLiteral(" \\n\\r\\t", " import ");
	}
	
	@Test public void testAnyChar() {
		parseStringLiteral("*/", "#", "/");
	}
	
	@Test public void testInt() {
		parseStringLiteral("123", "INT");
	}
	
	@Test public void testComplex() {
		parseStringLiteral(
				" import var id.id.* ", 
				" \\${ something . .* } ");
	}
	
	@Test public void testReference() {
		parseStringLiteral(
				"${something}", 
				"${ something }", 
				"${something. /* comment */ ^module}", 
				"${something.\nsomething // comment \n}");
	}
	
	@Test public void testReferences() {
		parseStringLiteral(
				"${something } ${ something.else}", 
				"${something}${else}");
	}
	
	@Test public void testIncompleteReference() {
		parseStringLiteral("${}", "${", "${something", "${something.");
	}
	
	@Test public void testMixed() {
		parseStringLiteral("import${something}", "$${something}", " ${something}}", "{${something}$");
	}
	
	protected void parseStringLiteral(String... literals) {
		for(String literal: literals) {
			String quoted = "'" + literal + "'";
			parseSuccessfully(quoted, quoted);
			quoted = '"' + literal + '"';
			parseSuccessfully(quoted, quoted);
		}
	}
	
	protected void parseSuccessfully(String input, String... expectedTokens) {
		CharStream stream = new ANTLRStringStream(input);
		lexer.setCharStream(stream);
		XtextTokenStream tokenStream = new XtextTokenStream(lexer, tokenDefProvider);
		List<?> tokens = tokenStream.getTokens();
		assertEquals(input, expectedTokens.length, tokens.size());
		for(int i = 0;i < tokens.size(); i++) {
			Token token = (Token) tokens.get(i);
			assertEquals(token.toString(), expectedTokens[i], token.getText());
		}
	}
}
