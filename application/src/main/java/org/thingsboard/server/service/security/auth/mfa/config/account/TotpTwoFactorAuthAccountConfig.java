/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.security.auth.mfa.config.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.thingsboard.server.service.security.auth.mfa.provider.TwoFactorAuthProviderType;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@ApiModel
@Data
public class TotpTwoFactorAuthAccountConfig implements TwoFactorAuthAccountConfig {

    @ApiModelProperty(value = "OTP auth URL used to generate a QR code to scan with an authenticator app. Must not be blank and must follow specific pattern.",
            example = "otpauth://totp/ThingsBoard:tenant@thingsboard.org?issuer=ThingsBoard&secret=FUNBIM3CXFNNGQR6ZIPVWHP65PPFWDII", required = true)
    @NotBlank(message = "OTP auth URL cannot be blank")
    @Pattern(regexp = "otpauth://totp/(\\S+?):(\\S+?)\\?issuer=(\\S+?)&secret=(\\w+?)", message = "OTP auth url is invalid")
    private String authUrl;

    @Override
    public TwoFactorAuthProviderType getProviderType() {
        return TwoFactorAuthProviderType.TOTP;
    }

}
