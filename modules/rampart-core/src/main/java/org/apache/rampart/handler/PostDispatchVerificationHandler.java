/*
 * Copyright 2004,2005 The Apache Software Foundation.
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

package org.apache.rampart.handler;

import org.apache.axiom.om.OMElement;
import org.apache.axis2.AxisFault;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.HandlerDescription;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.Handler;
import org.apache.neethi.Assertion;
import org.apache.neethi.Policy;
import org.apache.neethi.PolicyEngine;
import org.apache.rampart.RampartMessageData;
import org.apache.ws.secpolicy.model.Binding;
import org.apache.ws.security.handler.WSHandlerConstants;

import java.util.Iterator;
import java.util.List;

/**
 * Handler to verify the message security after dispatch
 *
 */
public class PostDispatchVerificationHandler implements Handler {

    private HandlerDescription handlerDesc;
    
    /**
     * @see org.apache.axis2.engine.Handler#cleanup()
     */
    public void cleanup() {
    }

    /**
     * @see org.apache.axis2.engine.Handler#flowComplete(org.apache.axis2.context.MessageContext)
     */
    public void flowComplete(MessageContext msgContext) {
    }

    /**
     * @see org.apache.axis2.engine.Handler#getHandlerDesc()
     */
    public HandlerDescription getHandlerDesc() {
        return this.handlerDesc;
    }

    /**
     * @see org.apache.axis2.engine.Handler#getName()
     */
    public String getName() {
        return "Post dispatch security verification handler";
    }

    /**
     * @see org.apache.axis2.engine.Handler#getParameter(java.lang.String)
     */
    public Parameter getParameter(String name) {
        return this.handlerDesc.getParameter(name);
    }

    /**
     * @see org.apache.axis2.engine.Handler#init(org.apache.axis2.description.HandlerDescription)
     */
    public void init(HandlerDescription handlerDesc) {
        this.handlerDesc = handlerDesc;
    }

    /**
     * @see org.apache.axis2.engine.Handler#invoke(org.apache.axis2.context.MessageContext)
     */
    public InvocationResponse invoke(MessageContext msgContext)
            throws AxisFault {
        Policy policy = msgContext.getEffectivePolicy();
        
        
        if(msgContext.getProperty(RampartMessageData.KEY_RAMPART_POLICY) != null) {
            policy = (Policy)msgContext.getProperty(RampartMessageData.KEY_RAMPART_POLICY);
        }
        

        if(policy == null) {
            policy = msgContext.getEffectivePolicy();
        }
        
        if(policy == null) {
            Parameter param = msgContext.getParameter(RampartMessageData.KEY_RAMPART_POLICY);
            if(param != null) {
                OMElement policyElem = param.getParameterElement().getFirstElement();
                policy = PolicyEngine.getPolicy(policyElem);
            }
        }
        
        if(policy == null) {
            return InvocationResponse.CONTINUE;
        }
        
        Iterator alternatives = policy.getAlternatives();
        
        boolean securityPolicyPresent = false;
        if(alternatives.hasNext()) {
            List assertions = (List)alternatives.next();
            for (Iterator iterator = assertions.iterator(); iterator.hasNext();) {
                Assertion assertion = (Assertion) iterator.next();
                //Check for any *Binding assertion
                if (assertion instanceof Binding) {
                    securityPolicyPresent = true;
                }
            }
        }

        //Now check for security processing results if security policy is available
        if(securityPolicyPresent && msgContext.getProperty(WSHandlerConstants.RECV_RESULTS) == null) {
            throw new AxisFault("InvalidSecurity");
        }
        
        return InvocationResponse.CONTINUE;
        
    }

}
