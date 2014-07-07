/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.lens;

import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;

/**
 * Interface used to intercept the SyncContext as it passes through the computation.
 * 
 * It is mostly used in tests.
 * 
 * EXPERIMENTAL
 * 
 * @author Radovan Semancik
 *
 */
public interface LensDebugListener {
	
	public <F extends ObjectType, P extends ObjectType> void beforeSync(LensContext<F,P> context);

	public <F extends ObjectType, P extends ObjectType> void afterSync(LensContext<F,P> context);
	
}