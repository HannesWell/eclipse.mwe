/*******************************************************************************
 * Copyright (c) 2007, 2015 itemis AG (http://www.itemis.eu) and others.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.emf.mwe.tests.util;

import java.io.File;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.mwe.core.WorkflowContext;
import org.eclipse.emf.mwe.core.WorkflowContextDefaultImpl;
import org.eclipse.emf.mwe.core.issues.Issues;
import org.eclipse.emf.mwe.core.issues.IssuesImpl;
import org.eclipse.emf.mwe.core.monitor.NullProgressMonitor;
import org.eclipse.emf.mwe.utils.Reader;
import org.eclipse.emf.mwe.utils.StandaloneSetup;
import org.eclipse.emf.mwe.utils.Writer;
import org.junit.Assert;
import org.junit.Test;

public class WriterTest extends Assert {
	private String relative = "testmodel.ecore";
	private String tempfile = "file:/"+System.getProperty("java.io.tmpdir") +"/"+relative;

	@Test public void testWriteSimpleModel() throws Exception {
		ResourceSet rs = new ResourceSetImpl();
		Writer writer = new Writer();
		writer.setModelSlot("x");
		writer.setUri(tempfile);
		writer.setResourceSet(rs);
		new StandaloneSetup().setPlatformUri(new File("..").getAbsolutePath());
		
		WorkflowContext ctx = new WorkflowContextDefaultImpl();
		EPackage pack = EcoreFactory.eINSTANCE.createEPackage();
		pack.setName("test");
		pack.setNsURI("http://www.eclipse.org/oaw/writer/test");
		EClass clazz = EcoreFactory.eINSTANCE.createEClass();
		clazz.setName("TEST");
		pack.getEClassifiers().add(clazz);
		ctx.set("x", pack);
		writer.invoke(ctx, new NullProgressMonitor(), new IssuesImpl());
		EObject model1 = (EObject) ctx.get("x");
		assertNotNull(model1);
		
		// read in
		Reader r = new Reader();
		r.setFirstElementOnly(true);
		r.setModelSlot("y");
		r.setResourceSet(rs);
		r.setUri(tempfile);
		
		r.invoke(ctx, new NullProgressMonitor(), new IssuesImpl());
		EPackage pack2 = (EPackage) ctx.get("y");

		assertTrue(pack == pack2);
		new File(relative).delete();
		
	}
	
	@Test public void testIgnoreEmptySlot() {
		ResourceSet rs = new ResourceSetImpl();
		Writer writer = new Writer();
		writer.setModelSlot("x");
		writer.setUri(tempfile);
		writer.setResourceSet(rs);
		writer.setIgnoreEmptySlot(true);
		new StandaloneSetup().setPlatformUri(new File("..").getAbsolutePath());
		
		WorkflowContext ctx = new WorkflowContextDefaultImpl();
		Issues issues = new IssuesImpl(); 
		writer.checkConfiguration(issues);
		assertFalse(issues.hasErrors());
		// we cannot know about empty slot contents at this time
		assertFalse(issues.hasWarnings());
		writer.invoke(ctx, new NullProgressMonitor(), issues);
		assertFalse(issues.hasErrors());
		assertTrue(issues.hasWarnings());
		 		
		File f = new File(relative);
		assertFalse(f.exists());
	}

}
