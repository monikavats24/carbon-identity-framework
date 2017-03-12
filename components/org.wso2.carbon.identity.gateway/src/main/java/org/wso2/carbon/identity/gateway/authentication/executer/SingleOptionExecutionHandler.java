
/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.wso2.carbon.identity.gateway.authentication.executer;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.identity.gateway.api.request.GatewayRequest;
import org.wso2.carbon.identity.gateway.authentication.AbstractSequence;
import org.wso2.carbon.identity.gateway.authentication.AuthenticationResponse;
import org.wso2.carbon.identity.gateway.authentication.authenticator.ApplicationAuthenticator;
import org.wso2.carbon.identity.gateway.common.model.sp.AuthenticationStepConfig;
import org.wso2.carbon.identity.gateway.common.model.sp.IdentityProvider;
import org.wso2.carbon.identity.gateway.context.AuthenticationContext;
import org.wso2.carbon.identity.gateway.context.SequenceContext;
import org.wso2.carbon.identity.gateway.exception.AuthenticationHandlerException;
import org.wso2.carbon.identity.gateway.request.AuthenticationRequest;
import org.wso2.carbon.identity.gateway.request.ClientAuthenticationRequest;

public class SingleOptionExecutionHandler extends AbstractExecutionHandler {

    private static final Logger log = LoggerFactory.getLogger(SingleOptionExecutionHandler.class);

    @Override
    public AuthenticationResponse execute(AuthenticationContext authenticationContext) throws AuthenticationHandlerException {

        ApplicationAuthenticator applicationAuthenticator = null ;
        SequenceContext sequenceContext = authenticationContext.getSequenceContext();
        SequenceContext.StepContext currentStepContext = sequenceContext.getCurrentStepContext();
        AbstractSequence sequence = authenticationContext.getSequence();
        AuthenticationRequest authenticationRequest = (AuthenticationRequest)authenticationContext.getIdentityRequest();

        if (currentStepContext != null) {
            if (StringUtils.isNotBlank(currentStepContext.getAuthenticatorName())
                    && StringUtils.isNotBlank(currentStepContext.getIdentityProviderName())) {
                applicationAuthenticator = getApplicationAuthenticator(currentStepContext.getAuthenticatorName());
            }else if(StringUtils.isNotBlank(authenticationRequest.getAuthenticatorName()) && StringUtils.isNotBlank
                    (authenticationRequest.getIdentityProviderName())){
                applicationAuthenticator = getApplicationAuthenticator(authenticationRequest.getAuthenticatorName());
                currentStepContext.setIdentityProviderName(authenticationRequest.getIdentityProviderName());
                currentStepContext.setAuthenticatorName(authenticationRequest.getAuthenticatorName());
            }
        }else{
            currentStepContext = sequenceContext.addStepContext();
            if(authenticationRequest instanceof ClientAuthenticationRequest && StringUtils.isNotBlank(authenticationRequest
                    .getAuthenticatorName())
                    &&
                    StringUtils.isNotBlank
                    (authenticationRequest.getIdentityProviderName())){
                applicationAuthenticator = getApplicationAuthenticator(authenticationRequest.getAuthenticatorName());
                currentStepContext.setIdentityProviderName(authenticationRequest.getIdentityProviderName());
                currentStepContext.setAuthenticatorName(authenticationRequest.getAuthenticatorName());
            }
        }

        if(applicationAuthenticator == null){
            AuthenticationStepConfig authenticationStepConfig = sequence.getAuthenticationStepConfig(currentStepContext.getStep());
            IdentityProvider identityProvider = authenticationStepConfig.getIdentityProviders().get(0);
            currentStepContext.setAuthenticatorName(identityProvider.getAuthenticatorName());
            currentStepContext.setIdentityProviderName(identityProvider.getIdentityProviderName());
            applicationAuthenticator = getApplicationAuthenticator(identityProvider.getAuthenticatorName());
        }

        if(applicationAuthenticator == null){
            throw new AuthenticationHandlerException("Authenticator not found.");
        }
        AuthenticationResponse response = null;
        try {
            response = applicationAuthenticator.process(authenticationContext);
            if(AuthenticationResponse.AUTHENTICATED.equals(response)){
                sequenceContext.getCurrentStepContext().setStatus(SequenceContext.Status.AUTHENTICATED);
            }else{
                sequenceContext.getCurrentStepContext().setStatus(SequenceContext.Status.INCOMPLETE);
            }
        } catch (AuthenticationHandlerException e) {
            //retry stuff

            //If retry == true , then change status INITIAL and call recursive
            //To check enable/disable to retry from authetnicator and try.
            //Retry count for only per step.
            currentStepContext.setStatus(SequenceContext.Status.FAILED);
            if(applicationAuthenticator.isRetryEnable(authenticationContext)){
                AuthenticationStepConfig authenticationStepConfig = sequence.getAuthenticationStepConfig(currentStepContext.getStep());
                int retryCount = authenticationStepConfig.getRetryCount();
                if(currentStepContext.getRetryCount() <= retryCount){
                    currentStepContext.setRetryCount(retryCount + 1);
                    return execute(authenticationContext);
                }

            }else{

                throw e;
            }


        }

        return response;
    }

    @Override
    public boolean canHandle(AuthenticationContext authenticationContext) {
        try {
            return canHandle(authenticationContext, ExecutionStrategy.SINGLE.toString());
        } catch (AuthenticationHandlerException e) {
            log.error("Error occurred while trying to check the can handle for execution strategy, " + e.getMessage());
        }
        return false;
    }
}